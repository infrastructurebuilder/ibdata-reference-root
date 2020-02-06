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
import static org.infrastructurebuilder.data.IBDataConstants.METADATA;
import static org.infrastructurebuilder.data.IBDataConstants.SCHEMA;
import static org.infrastructurebuilder.data.IBDataException.cet;
import static org.infrastructurebuilder.data.model.IBDataModelUtils.readSchemaFromURL;
import static org.infrastructurebuilder.data.model.IBDataModelUtils.writeSchemaToPath;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.infrastructurebuilder.data.AbstractIBSchemaSource;
import org.infrastructurebuilder.data.IBDataConstants;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBSchemaSourceSupplier;
import org.infrastructurebuilder.data.Metadata;
import org.infrastructurebuilder.data.model.io.xpp3.PersistedIBSchemaXpp3Reader;
import org.infrastructurebuilder.data.model.io.xpp3.PersistedIBSchemaXpp3Writer;
import org.infrastructurebuilder.util.BasicCredentials;
import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;
import org.infrastructurebuilder.util.files.DefaultIBResource;
import org.infrastructurebuilder.util.files.IBResource;

@Named("urls")
public class URLBackedIBSchemaSourceSupplierMapper extends AbstractIBSchemaSourceSupplierMapper<List<URL>> {

  @Inject
  public URLBackedIBSchemaSourceSupplierMapper(IBRuntimeUtils ibr) {
    super(ibr);
  }

  @Override
  public boolean respondsTo(IBDataSchemaIngestionConfig v) {
    return v.getUrls().isPresent();
  }

  @Override
  public Optional<IBSchemaSourceSupplier> getSupplierFor(IBDataSchemaIngestionConfig v) {
    return of(
        // Always present if respondsTo works properly
        new DefaultIBSchemaSourceSupplier(
            // TempID
            v.getTemporaryId(),
            // Schema source
            new URLListIBSchemaSource(getRuntimeUtils())
                // Configured with the ingestion config
                .configure(v.asConfigMap()),
            // workingPath
            () -> getWorkingPath(), v));
  }

  public class URLListIBSchemaSource extends AbstractIBSchemaSource<List<URL>> {

    public URLListIBSchemaSource(IBRuntimeUtils ibr) {
      super(ibr);
    }

    private URLListIBSchemaSource(IBRuntimeUtils ibr
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
        , List<URL> parameter) {
      super(ibr, id, name, desc, creds, empty(), metadata, config, parameter);
    }

    @Override
    public URLListIBSchemaSource configure(ConfigMap config) {
      String id = config.getOrDefault(TEMPORARYID, "default");
      Optional<Metadata> metadata2 = of(config.get(METADATA));
//      Optional<Checksum> checksum2 = empty(); // No checksum for inlines
      Optional<BasicCredentials> creds2 = ofNullable(config.get(CREDENTIALS));
      Optional<String> desc2 = config.getOptionalString(DESCRIPTION);
      Optional<String> name2 = config.getOptionalString(NAME);
      List<URL> param = config.get(IBDataConstants.URLS);
      return new URLListIBSchemaSource(getRuntimeUtils(), id, name2, desc2, creds2, metadata2, of(config), param);
    }

    @Override
    protected Map<String, IBResource> getInstance(IBRuntimeUtils ibr, List<URL> urls) {
      if (requireNonNull(urls, "Supplied URL List for read schema").size() != 1)
        throw new IBDataException("Currently system only handles a single URL at at time");
      URL u = urls.get(0);
      Path path = writeSchemaToPath.apply(ibr, readSchemaFromURL.apply(u));
      // URL-based schemas only have the persisted schema as an asset
      Map<String, IBResource> r = new HashMap<>();
      r.put(DEFAULT, new DefaultIBResource(path, new Checksum(path), of(IBDATA_SCHEMA)));
      return unmodifiableMap(r);
    }

  }

}
