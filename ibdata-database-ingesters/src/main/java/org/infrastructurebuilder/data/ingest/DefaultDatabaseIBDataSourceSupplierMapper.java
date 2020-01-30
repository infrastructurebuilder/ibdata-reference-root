/**
 * Copyright © 2019 admin (admin@infrastructurebuilder.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.infrastructurebuilder.data.ingest;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.infrastructurebuilder.IBConstants.*;
import static org.infrastructurebuilder.data.IBDataConstants.*;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;
import static org.infrastructurebuilder.data.IBDataConstants.QUERY;
import static org.infrastructurebuilder.data.IBDataConstants.SCHEMA;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;

import org.apache.avro.Schema;
import org.infrastructurebuilder.data.AbstractIBDataSource;
import org.infrastructurebuilder.data.IBDataDatabaseDriverSupplier;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBDataSourceSupplier;
import org.infrastructurebuilder.data.IBDataStreamIdentifier;
import org.infrastructurebuilder.data.IBDatabaseDialectMapper;
import org.infrastructurebuilder.data.JooqAvroRecordWriterSupplier;
import org.infrastructurebuilder.data.Metadata;
import org.infrastructurebuilder.util.BasicCredentials;
import org.infrastructurebuilder.util.CredentialsFactory;
import org.infrastructurebuilder.util.DefaultURLAndCreds;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.URLAndCreds;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.DefaultConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.IBResource;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;

@Named("jdbc-jooq")
public class DefaultDatabaseIBDataSourceSupplierMapper extends AbstractIBDataSourceSupplierMapper<Schema> {
  public final static List<String> HEADERS = asList("jdbc:");
  public static final String DEFAULT_NAMESPACE = "org.infrastructurebuilder.data";

  private final JooqAvroRecordWriterSupplier jrws;
  private final IBDatabaseDialectMapper dm;

  @Inject
  public DefaultDatabaseIBDataSourceSupplierMapper(@Named(IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps,
      LoggerSupplier l, TypeToExtensionMapper t2e, JooqAvroRecordWriterSupplier jrws, CredentialsFactory cf,
      IBDatabaseDialectMapper dm) {
    super(requireNonNull(l).get(), requireNonNull(t2e), wps, cf);
    this.jrws = requireNonNull(jrws);
    this.dm = requireNonNull(dm);
  }

  @Override
  public List<String> getHeaders() {
    return HEADERS;
  }

  @Override
  public IBDataSourceSupplier<Schema> getSupplierFor(IBDataStreamIdentifier v) {
    String temporaryId = v.getTemporaryId().orElse(null);
    URLAndCreds jdbcURL = v.getURLAndCreds().get();
//    new DefaultURLAndCreds(v.getUrl().get(), v.getCredentialsQuery());
    Optional<BasicCredentials> creds = v.getCredentialsQuery()
        .flatMap(q -> getCredentialsFactory().getCredentialsFor(q));
    IBDataDatabaseDriverSupplier supplier = dm.getSupplierForURL(jdbcURL)
        .orElseThrow(() -> new IBDataException("No  IBDataDatabaseDriverSupplier available for " + jdbcURL));
    return new DefaultIBDataSourceSupplier<Schema>(temporaryId // temp id TODO Fixme
        , new DefaultDatabaseIBDataSource( // The supplier instance
            getWorkingPathSupplier() // PathSupplier
            , getLog() // Logger
            , temporaryId // Temporary id (TODO to be remapped )
            , jdbcURL // source
            , false // Databases don't have archives to expand
            , ofNullable(v.getChecksum()) // This checksum
            , of(v.getMetadata()) // Metadata
            , empty() // Additional config?
            , empty() // Namespace (FIXME where does namespace come from?)
            , v.getName() // Name (used as the table)
            , v.getDescription() // desc
            , getMapper() // Type mapper
            , this.jrws // Jooq writer (TODO remove the need for this)
            , supplier // The driver supplier
        ) // That's the source supplier
        , getWorkingPathSupplier() // Still have to supply this upwards
        , creds // creds again
    );
  }

  public class DefaultDatabaseIBDataSource extends AbstractIBDataSource<Schema> implements AutoCloseable {

    private final TypeToExtensionMapper t2e;

    private List<IBResource> read;

//    private final IBDataAvroUtilsSupplier aus;
    private final JooqAvroRecordWriterSupplier jwrs;

    private final IBDataDatabaseDriverSupplier supplier;

    public DefaultDatabaseIBDataSource(PathSupplier wps, Logger l, String tempId, URLAndCreds source, boolean expand,
        Optional<Checksum> checksum, Optional<Metadata> metadata, Optional<ConfigMap> additionalConfig,
        Optional<String> namespace, Optional<String> name, Optional<String> description, TypeToExtensionMapper t2e,
        /* IBDataAvroUtilsSupplier jds, */JooqAvroRecordWriterSupplier jrws, IBDataDatabaseDriverSupplier supplier) {

      super(wps, l, tempId, source, false /* Databases y'all */, namespace, name, description, checksum, metadata,
          additionalConfig, null);
      this.supplier = requireNonNull(supplier);
      this.t2e = t2e;
      this.jwrs = requireNonNull(jrws);
    }

    private Connection conn;

    @Override
    public void close() throws Exception {
      if (conn != null)
        conn.close();
    }

    @Override
    public DefaultDatabaseIBDataSource configure(ConfigMap config) {
      return new DefaultDatabaseIBDataSource(getWorkingPathSupplier(), getLog(), getId(), getSource(), false,
          getChecksum(), getMetadata(), of(config), getNamespace(), getName(), getDescription(), t2e,
          (JooqAvroRecordWriterSupplier) this.jwrs.configure(new DefaultConfigMapSupplier(config)), this.supplier);
    }

    @Override
    public List<IBResource> getInstance(PathSupplier workingPath, Schema in) {
      if (conn == null) {
        DataSource odss = supplier.getDataSourceSupplier(getSource()).map(Supplier::get)
            .orElseThrow(() -> new IBDataException("No DataSource available for " + getSource()));
        conn = IBDataException.cet.withReturningTranslation(() -> odss.getConnection());
      }
      ConfigMap cfg = getConfig();
      String sql = cfg.getString(QUERY);
      SQLDialect dialect = SQLDialect.valueOf(cfg.getString(DIALECT));
      if (!getName().isPresent())
        throw new IBDataException("Name is required to produce record");
      String recordName = getName().get();
      return ofNullable(getSource().getUrl()).map(source -> { // Never null
        if (this.read == null) {
          Optional<String> sString;
//          if (source.startsWith("jdbc:")) {  // Always true at this point

            DSLContext create = DSL.using(conn, dialect);
            final Result<Record> result;
            final Result<Record> firstResult = create.fetch(sql);
            sString = ofNullable(cfg.getString(SCHEMA));
            String namespace = getNamespace().orElse(DEFAULT_NAMESPACE);
//            Schema avroSchema = sString
//                // Either we already have a schema
//                .map(qq -> this.aus.get().avroSchemaFromString(qq))
//                // Or we have to produce one
//                .orElse(IBDataJooqUtils.schemaFromRecordResults(getLog(), namespace, recordName,
//                    getDescription().orElse(""), firstResult));
            result = (!sString.isPresent()) ? create.fetch(sql) : firstResult; // Read again if we had to create the
                                                                               // schema
            getLog().info("Reading data from dataset");
            read = singletonList(this.jwrs.get().writeRecords(result));
// Removed for always-true-ness
//          } else
//            throw new IBDataException("Processor " + getId() + " cannot handle protocol for " + source);
        }
        return read;
      }).orElse(emptyList());
    }

    @Override
    public Optional<String> getMimeType() {
      return of(AVRO_BINARY);
    }
  }

}
