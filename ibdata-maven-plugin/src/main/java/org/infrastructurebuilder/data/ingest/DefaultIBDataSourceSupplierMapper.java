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

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.infrastructurebuilder.IBConstants.IBDATA_PREFIX;
import static org.infrastructurebuilder.IBConstants.IBDATA_SUFFIX;
import static org.infrastructurebuilder.data.IBDataException.cet;
import static org.infrastructurebuilder.util.files.DefaultIBChecksumPathType.copyToDeletedOnExitTempChecksumAndPath;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.data.AbstractIBDataSource;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBDataSource;
import org.infrastructurebuilder.data.IBDataSourceSupplier;
import org.infrastructurebuilder.data.IBDataStreamIdentifier;
import org.infrastructurebuilder.util.BasicCredentials;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;
import org.slf4j.Logger;
import org.w3c.dom.Document;

@Named
public class DefaultIBDataSourceSupplierMapper extends AbstractIBDataSourceSupplierMapper {
  public final static List<String> HEADERS = Arrays.asList("http://", "https://", "file:", "zip:", "jar:");

  @Inject
  public DefaultIBDataSourceSupplierMapper(LoggerSupplier l, TypeToExtensionMapper t2e) {
    super(requireNonNull(l).get(), requireNonNull(t2e));
  }

  public List<String> getHeaders() {
    return HEADERS;
  }

  @Override
  public IBDataSourceSupplier getSupplierFor(String temporaryId, IBDataStreamIdentifier v) {
    return new DefaultIBDataSourceSupplier(temporaryId,
        new DefaultIBDataSource(getLog(),
            v.getURL().orElseThrow(() -> new IBDataException("No url for " + temporaryId)), v.getName(),
            v.getDescription(), ofNullable(v.getChecksum()), of(v.getMetadataAsDocument()), ofNullable(v.getMimeType()),
            getMapper()));
  }


  public class DefaultIBDataSource extends AbstractIBDataSource {

    private final Path targetPath;
    private final Path cacheDirectory;
    private final TypeToExtensionMapper t2e;
    private final Optional<String> mimeType;

    private List<IBChecksumPathType> read;

    private DefaultIBDataSource(Logger log, String id, String source, Optional<BasicCredentials> creds,
        Optional<Checksum> checksum, Optional<Document> metadata, Optional<ConfigMap> additionalConfig, Path targetPath,
        Optional<String> name, Optional<String> description, Path cacheDir, Optional<String> mimeType,
        TypeToExtensionMapper t2e) {

      super(log, id, source, name, description, creds, checksum, metadata, additionalConfig);
      this.targetPath = targetPath;
      this.cacheDirectory = cacheDir;
      this.t2e = t2e;
      this.mimeType = mimeType;
    }

    public DefaultIBDataSource(Logger log, String source, Optional<String> name, Optional<String> description,
        Optional<Checksum> checksum, Optional<Document> metadata, Optional<String> targetType,
        TypeToExtensionMapper t2e) {
      this(log, randomUUID().toString(), source, empty(), checksum, metadata, empty(), null, name, description, null,
          targetType, t2e);
    }

    @Override
    public IBDataSource withAdditionalConfig(ConfigMap config) {
      return new DefaultIBDataSource(getLog(), getId(), getSourceURL(), getCredentials(), getChecksum(), getMetadata(),
          of(config), config.get(TARGET_PATH), getName(), getDescription(), config.get(CACHE_DIR), getMimeType(), t2e);
    }

    @Override
    public List<IBChecksumPathType> get() {
      return ofNullable(targetPath).map(target -> {
        if (this.read == null) {
          IBChecksumPathType localRead;
          URL src = cet.withReturningTranslation(() -> new URL(source));
          switch (src.getProtocol()) {
          case "http":
          case "https":
            WGetter wget = new WGetter(getLog(), getCredentials(), this.t2e);
            localRead = wget.collectCacheAndCopyToChecksumNamedFile(
                // Target directory
                targetPath,
                // URL
                src,
                // "Optional" checksum
                checksum,
                // Location of the cacheDirectory
                cacheDirectory,
                // Mime type from supplied value (not calculated value)
                this.mimeType,
                // Slightly insane defaults
                false, 3, 0, false).orElseThrow(() -> new IBDataException("Could not read "));
            break;
          case "file":
          case "zip":
            try (InputStream ins = src.openStream()) {
              localRead = cet
                  .withReturningTranslation(() -> copyToDeletedOnExitTempChecksumAndPath(ofNullable(targetPath),
                      IBDATA_PREFIX, IBDATA_SUFFIX, ins));

            } catch (IOException e) {
              throw new IBDataException(e);
            }
            break;
          default:
            throw new IBDataException("Default processor cannot handle protocol " + source);
          }
          read = Arrays.asList(localRead);
        }
        return read;
      }).orElse(Collections.emptyList());
    }

  }

}
