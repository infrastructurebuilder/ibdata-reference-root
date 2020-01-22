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
import static org.infrastructurebuilder.IBConstants.AVRO_BINARY;
import static org.infrastructurebuilder.data.IBDataConstants.DIALECT;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;
import static org.infrastructurebuilder.data.IBDataConstants.NAMESPACE;
import static org.infrastructurebuilder.data.IBDataConstants.QUERY;
import static org.infrastructurebuilder.data.IBDataConstants.SCHEMA;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.avro.Schema;
import org.infrastructurebuilder.data.AbstractIBDataSource;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBDataSourceSupplier;
import org.infrastructurebuilder.data.IBDataStreamIdentifier;
import org.infrastructurebuilder.data.JooqAvroRecordWriterSupplier;
import org.infrastructurebuilder.data.Metadata;
import org.infrastructurebuilder.util.BasicCredentials;
import org.infrastructurebuilder.util.DefaultBasicCredentials;
import org.infrastructurebuilder.util.LoggerSupplier;
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

  @Inject
  public DefaultDatabaseIBDataSourceSupplierMapper(@Named(IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps,
      LoggerSupplier l, TypeToExtensionMapper t2e, JooqAvroRecordWriterSupplier jrws) {
    super(requireNonNull(l).get(), requireNonNull(t2e), wps);
    this.jrws = requireNonNull(jrws);
  }

  @Override
  public List<String> getHeaders() {
    return HEADERS;
  }

  @Override
  public IBDataSourceSupplier<Schema> getSupplierFor(IBDataStreamIdentifier v) {
    String temporaryId = v.getTemporaryId().orElse(null);
    return new DefaultIBDataSourceSupplier<Schema>(temporaryId,
        new DefaultDatabaseIBDataSource(getWorkingPathSupplier(), getLog(), temporaryId,
            v.getUrl().orElseThrow(() -> new IBDataException("No url for " + temporaryId)), false, empty(),
            ofNullable(v.getChecksum()), of(v.getMetadata()), empty(), v.getName(), v.getDescription(), getMapper(),
            /* this.aus, */ this.jrws),
        getWorkingPathSupplier());
  }

  public class DefaultDatabaseIBDataSource extends AbstractIBDataSource<Schema> implements AutoCloseable {

    private final TypeToExtensionMapper t2e;

    private List<IBResource> read;

//    private final IBDataAvroUtilsSupplier aus;
    private final JooqAvroRecordWriterSupplier jwrs;

    public DefaultDatabaseIBDataSource(PathSupplier wps, Logger l, String tempId, String source, boolean expand,
        Optional<BasicCredentials> creds, Optional<Checksum> checksum, Optional<Metadata> metadata,
        Optional<ConfigMap> additionalConfig, Optional<String> name, Optional<String> description,
        TypeToExtensionMapper t2e, /* IBDataAvroUtilsSupplier jds, */JooqAvroRecordWriterSupplier jrws) {

      super(wps, l, tempId, source, false /* Databases y'all */, name, description, creds, checksum, metadata,
          additionalConfig, null);
      this.t2e = t2e;
//      this.aus = requireNonNull(jds);
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
      return new DefaultDatabaseIBDataSource(getWorkingPathSupplier(), getLog(), getId(), getSourceURL(), false,
          getCredentials(), getChecksum(), getMetadata(), of(config), getName(), getDescription(), t2e,
          (JooqAvroRecordWriterSupplier) this.jwrs.configure(new DefaultConfigMapSupplier(config)));
    }

    @Override
    public List<IBResource> getInstance(PathSupplier workingPath, Schema in) {
      if (conn == null) {
        String url = getSourceURL();
        BasicCredentials bc = getCredentials().orElse(new DefaultBasicCredentials("SA", empty()));
        conn = IBDataException.cet.withReturningTranslation(
            () -> DriverManager.getConnection(url, bc.getKeyId(), bc.getSecret().orElse(null)));

      }
      ConfigMap cfg = getConfig();
      String sql = cfg.getString(QUERY);
      SQLDialect dialect = SQLDialect.valueOf(cfg.getString(DIALECT));
      if (!getName().isPresent())
        throw new IBDataException("Name is required to produce record");
      String recordName = getName().get();
      return ofNullable(getSourceURL()).map(source -> {
        if (this.read == null) {
          Optional<String> sString;
          if (source.startsWith("jdbc:")) {
            DSLContext create = DSL.using(conn, dialect);
            final Result<Record> result;
            final Result<Record> firstResult = create.fetch(sql);
            sString = ofNullable(cfg.getString(SCHEMA));
            String namespace = cfg.getOrDefault(NAMESPACE, DEFAULT_NAMESPACE);
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
          } else
            throw new IBDataException("Processor " + getId() + " cannot handle protocol for " + source);
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
