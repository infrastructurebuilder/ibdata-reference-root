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
import static java.util.Optional.ofNullable;
import static org.infrastructurebuilder.IBConstants.CREDENTIALS;
import static org.infrastructurebuilder.IBConstants.DEFAULT;
import static org.infrastructurebuilder.IBConstants.DESCRIPTION;
import static org.infrastructurebuilder.IBConstants.IBDATA_SCHEMA;
import static org.infrastructurebuilder.IBConstants.NAME;
import static org.infrastructurebuilder.IBConstants.TEMPORARYID;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;
import static org.infrastructurebuilder.data.IBDataConstants.METADATA;
import static org.infrastructurebuilder.data.IBDataConstants.SCHEMA;
import static org.infrastructurebuilder.data.IBDataException.cet;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.infrastructurebuilder.data.AbstractIBSchemaSource;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBSchemaSourceSupplier;
import org.infrastructurebuilder.data.Metadata;
import org.infrastructurebuilder.data.model.io.xpp3.PersistedIBSchemaXpp3Reader;
import org.infrastructurebuilder.data.model.io.xpp3.PersistedIBSchemaXpp3Writer;
import org.infrastructurebuilder.util.BasicCredentials;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.DefaultIBResource;
import org.infrastructurebuilder.util.files.IBResource;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;
import org.slf4j.Logger;

@Named("database")
public class DefaultDatabaseIBSchemaSourceSupplierMapper extends AbstractIBSchemaSourceSupplierMapper<Xpp3Dom> {

  @Inject
  public DefaultDatabaseIBSchemaSourceSupplierMapper(LoggerSupplier log, TypeToExtensionMapper mapper,
      @Named(IBDATA_WORKING_PATH_SUPPLIER) PathSupplier workingPathSupplier) {
    super(log, mapper, workingPathSupplier);
  }

  @Override
  public boolean respondsTo(IBDataSchemaIngestionConfig v) {
    return v.getJDBCQuery().isPresent();
  }

  @Override
  public Optional<IBSchemaSourceSupplier> getSupplierFor(IBDataSchemaIngestionConfig v) {
    return of(
        // Always present if respondsTo works properly
        new DefaultIBSchemaSourceSupplier(
            // TempID
            v.getTemporaryId(),
            // Schema source
            new DatabaseQueryIBSchemaSource(() -> getWorkingPath(), getLog())
                // Configured with the ingestion config
                .configure(v.asConfigMap()),
            // workingPath
            () -> getWorkingPath(), v));
  }

  public class DatabaseQueryIBSchemaSource extends AbstractIBSchemaSource<Xpp3Dom> {

    public DatabaseQueryIBSchemaSource(PathSupplier wps, Logger logger) {
      super(wps, logger);
    }

    private DatabaseQueryIBSchemaSource(
        // Working path supplier
        PathSupplier workingPathSupplier
        // logger
        , Logger logger
        // id
        , String id
        // name
        , Optional<String> name
        // desc
        , Optional<String> desc
        // creds
        , Optional<BasicCredentials> creds
//        // checksum always empty
//        , Optional<Checksum> checksum
        // metadata
        , Optional<Metadata> metadata
        // config
        , Optional<ConfigMap> config
        // And the param supplied
        , Xpp3Dom parameter) {
      super(workingPathSupplier, logger, id, name, desc, creds, empty(), metadata, config, parameter);
    }

    @Override
    public DatabaseQueryIBSchemaSource configure(ConfigMap config) {
      String id = config.getOrDefault(TEMPORARYID, "default");
      Optional<Metadata> metadata2 = of(config.get(METADATA));
//      Optional<Checksum> checksum2 = empty(); // No checksum for inlines
      Optional<BasicCredentials> creds2 = ofNullable(config.get(CREDENTIALS));
      Optional<String> desc2 = config.getOptionalString(DESCRIPTION);
      Optional<String> name2 = config.getOptionalString(NAME);
      Xpp3Dom param = config.get(SCHEMA);
      return new DatabaseQueryIBSchemaSource(getWorkingPathSupplier(), getLog(), id, name2, desc2, creds2, metadata2,
          of(config), param);
    }

    @Override
    protected Map<String, IBResource> getInstance(PathSupplier workingPath, Xpp3Dom inline) {
      Xpp3Dom j = ofNullable(requireNonNull(inline, "Supplied Xpp3Dom for inline schema").getChild("schema"))
          .orElseThrow(() -> new IBDataException("The child of <inline> must be a <schema/>"));
      String in = j.toString();
      Path path = workingPath.get().resolve(UUID.randomUUID().toString() + ".xml");
      // We read it as a string, clone it, then write the clone out to a path
      try (Writer w = Files.newBufferedWriter(path)) {
        new PersistedIBSchemaXpp3Writer().write(w, cet.withReturningTranslation(() ->
        // From new reader
        new PersistedIBSchemaXpp3Reader()
            // read
            .read(new StringReader(in))).clone());
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
