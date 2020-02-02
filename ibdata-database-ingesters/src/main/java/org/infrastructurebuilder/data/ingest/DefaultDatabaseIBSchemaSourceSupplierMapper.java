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

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.infrastructurebuilder.IBConstants.DEFAULT;
import static org.infrastructurebuilder.IBConstants.IBDATA_SCHEMA;
import static org.infrastructurebuilder.data.IBDataException.cet;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;

import org.infrastructurebuilder.data.AbstractIBSchemaSource;
import org.infrastructurebuilder.data.IBDataDatabaseDriverSupplier;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBDatabaseDialectMapper;
import org.infrastructurebuilder.data.IBSchemaSourceSupplier;
import org.infrastructurebuilder.data.Metadata;
import org.infrastructurebuilder.data.model.io.xpp3.PersistedIBSchemaXpp3Reader;
import org.infrastructurebuilder.data.model.io.xpp3.PersistedIBSchemaXpp3Writer;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;
import org.infrastructurebuilder.util.files.DefaultIBResource;
import org.infrastructurebuilder.util.files.IBResource;

@Named("database")
public class DefaultDatabaseIBSchemaSourceSupplierMapper extends AbstractIBSchemaSourceSupplierMapper<IBJDBCQuery> {

  private IBDatabaseDialectMapper ddm;

  @Inject
  public DefaultDatabaseIBSchemaSourceSupplierMapper(IBRuntimeUtils ibr, IBDatabaseDialectMapper ddM) {
    super(ibr);
    this.ddm = requireNonNull(ddM);
  }

  @Override
  public boolean respondsTo(IBDataSchemaIngestionConfig v) {
    return v.getJDBCQuery().isPresent();
  }

  @Override
  public Optional<IBSchemaSourceSupplier> getSupplierFor(IBDataSchemaIngestionConfig v) {
    IBJDBCQuery q = v.getJDBCQuery().get();
    IBDataDatabaseDriverSupplier s = ddm.getSupplierForURL(q)
        .orElseThrow(() -> new IBDataException("No supplier for " + q.getUrl()));
    return of(
        // Always present if respondsTo works properly
        new DefaultIBSchemaSourceSupplier(
            // TempID
            v.getTemporaryId(),
            // Schema source
            new DatabaseQueryIBSchemaSource(getRuntimeUtils()
            // id
                , v.getTemporaryId()
                // name
                , v.getName()
                // desc
                , v.getDescription()
                // meta
                , Optional.of(v.getMetadata())
                // config (useless)
                , Optional.empty()
                // URLAndCreds
                , q
                // DDS
                , s)
            // workingPath
            , () -> getWorkingPath(), v));
  }

  public class DatabaseQueryIBSchemaSource extends AbstractIBSchemaSource<IBJDBCQuery> {

    private IBDataDatabaseDriverSupplier dds;

    public DatabaseQueryIBSchemaSource(IBRuntimeUtils ibr) {
      super(ibr);
    }

    public DatabaseQueryIBSchemaSource(IBRuntimeUtils ibr
    // id
        , String id
        // name
        , Optional<String> name
        // desc
        , Optional<String> desc
        // metadata
        , Optional<Metadata> metadata
        // config
        , Optional<ConfigMap> config
        // And the param supplied
        , IBJDBCQuery parameter
        // dds
        , IBDataDatabaseDriverSupplier dds) {
      super(ibr, id, name, desc, ibr.getCredentialsFor(parameter), empty(), metadata, config, parameter);
      this.dds = Objects.requireNonNull(dds);
    }

    @Override
    protected Map<String, IBResource> getInstance(IBRuntimeUtils ibr, IBJDBCQuery db) {

      Supplier<DataSource> s = dds.getDataSourceSupplier(db)
          .orElseThrow(() -> new IBDataException("No data source supplier for " + db.getUrl()));
      String in = requireNonNull(db, "URL and creds").getUrl();
      Path path = ibr.getWorkingPath().resolve(UUID.randomUUID().toString() + ".xml");
      // We read it as a string, clone it, then write the clone out to a path
      try (Writer w = Files.newBufferedWriter(path)) {
        new PersistedIBSchemaXpp3Writer().write(w,
            // From new reader
            cet.withReturningTranslation(() -> new PersistedIBSchemaXpp3Reader().read(new StringReader(in)))
                // Then clone to do the clone-based updates from the model
                .clone());
        // Inline schemas only have the persisted schema as an asset
        Map<String, IBResource> r = new HashMap<>();
        r.put(DEFAULT, new DefaultIBResource(path, new Checksum(path), of(IBDATA_SCHEMA)));
        return unmodifiableMap(r);
      } catch (IOException e) {
        throw new IBDataException(String.format("Failed to persist InlineIBSchema to %s", path.toString()), e);
      }
    }

  }

}
