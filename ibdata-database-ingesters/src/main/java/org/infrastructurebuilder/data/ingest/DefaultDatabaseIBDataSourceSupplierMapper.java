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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.infrastructurebuilder.IBConstants.AVRO_BINARY;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.avro.Schema;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.infrastructurebuilder.data.AbstractIBDataSource;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBDataJooqUtils;
import org.infrastructurebuilder.data.IBDataSourceSupplier;
import org.infrastructurebuilder.data.IBDataStreamIdentifier;
import org.infrastructurebuilder.data.JooqAvroRecordWriterSupplier;
import org.infrastructurebuilder.data.Metadata;
import org.infrastructurebuilder.util.BasicCredentials;
import org.infrastructurebuilder.util.DefaultBasicCredentials;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;

@Named("jdbc-jooq")
public class DefaultDatabaseIBDataSourceSupplierMapper extends AbstractIBDataSourceSupplierMapper {
  public final static List<String> HEADERS = Arrays.asList("jdbc:");
  public static final String DEFAULT_NAMESPACE = "org.infrastructurebuilder.data";

  private static final String NO_QUERY_MSG = "No additional config available (query)";

  public static final String QUERY = "query";
  public static final String DIALECT = "dialect";
  public static final String SCHEMA = "schema"; // "Optional" (sort of )

  public static final String NAMESPACE = "namespace";
//  private final IBDataAvroUtilsSupplier aus;
  private final JooqAvroRecordWriterSupplier jrws;

  @Inject
  public DefaultDatabaseIBDataSourceSupplierMapper(LoggerSupplier l, TypeToExtensionMapper t2e,
      @Named(IBDATA_WORKING_PATH_SUPPLIER) PathSupplier workingPathSupplier, /* IBDataAvroUtilsSupplier gds, */
      JooqAvroRecordWriterSupplier jrws) {
    super(requireNonNull(l).get(), requireNonNull(t2e), workingPathSupplier);
//    this.aus = requireNonNull(gds);
    this.jrws = requireNonNull(jrws);
  }

  public List<String> getHeaders() {
    return HEADERS;
  }

  @Override
  public IBDataSourceSupplier getSupplierFor(String temporaryId, IBDataStreamIdentifier v) {
    return new DefaultIBDataSourceSupplier(temporaryId,
        new DefaultDatabaseIBDataSource(getLog(), temporaryId,
            v.getUrl().orElseThrow(() -> new IBDataException("No url for " + temporaryId)), false, empty(),
            ofNullable(v.getChecksum()), of(v.getMetadata()), empty(), null, v.getName(), v.getDescription(),
            getMapper(), /* this.aus, */ this.jrws),
        getWorkingPath());
  }

  public class DefaultDatabaseIBDataSource extends AbstractIBDataSource implements AutoCloseable {

    private final Path targetPath;
    private final TypeToExtensionMapper t2e;

    private List<IBChecksumPathType> read;

//    private final IBDataAvroUtilsSupplier aus;
    private final JooqAvroRecordWriterSupplier jwrs;

    public DefaultDatabaseIBDataSource(Logger l, String tempId, String source, boolean expand,
        Optional<BasicCredentials> creds, Optional<Checksum> checksum, Optional<Metadata> metadata,
        Optional<ConfigMap> additionalConfig, Path targetPath, Optional<String> name, Optional<String> description,
        TypeToExtensionMapper t2e, /* IBDataAvroUtilsSupplier jds, */JooqAvroRecordWriterSupplier jrws) {

      super(l, tempId, source, false /* Databases y'all */, name, description, creds, checksum, metadata,
          additionalConfig);
      this.targetPath = targetPath;
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
      return new DefaultDatabaseIBDataSource(getLog(), getId(), getSourceURL(), false, getCredentials(), getChecksum(),
          getMetadata(), of(config), getWorkingPath(), getName(), getDescription(), t2e,
          /* (IBDataAvroUtilsSupplier) this.aus.configure(config), */
          (JooqAvroRecordWriterSupplier) this.jwrs.configure(config));
    }

    @Override
    public List<IBChecksumPathType> getInstance() {
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
