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

import static java.util.Optional.ofNullable;
import static org.infrastructurebuilder.IBConstants.IBDATA_PREFIX;
import static org.infrastructurebuilder.IBConstants.IBDATA_SUFFIX;
import static org.infrastructurebuilder.data.IBDataException.cet;
import static org.infrastructurebuilder.util.files.IBCoreReadDetectResponse.copyToDeletedOnExitTempChecksumAndPath;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.maven.plugin.logging.Log;
import org.infrastructurebuilder.data.AbstractIBDataSource;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.util.BasicCredentials;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;
import org.w3c.dom.Document;

public class DefaultIBDataSource extends AbstractIBDataSource {

  private final Path targetPath;
  private final String name;
  private final String description;
  private final Path cacheDirectory;
  private final Log log;
  private final TypeToExtensionMapper t2e;
  private final Optional<String> mimeType;

  private IBChecksumPathType read;

  @Override
  public final DefaultIBDataSource withTargetPath(Path targetPath) {
    return new DefaultIBDataSource(getId(), getSourceURL(), getCredentials(), getChecksum(), getMetadata(),
        Objects.requireNonNull(targetPath), name, description, cacheDirectory, log, mimeType, t2e);
  }

  @Override
  public final DefaultIBDataSource withName(String name) {
    return new DefaultIBDataSource(getId(), getSourceURL(), getCredentials(), getChecksum(), getMetadata(), targetPath,
        Objects.requireNonNull(name), description, cacheDirectory, log, mimeType, t2e);
  }

  @Override
  public final DefaultIBDataSource withDescription(String description) {
    return new DefaultIBDataSource(getId(), getSourceURL(), getCredentials(), getChecksum(), getMetadata(), targetPath,
        name, Objects.requireNonNull(description), cacheDirectory, log, mimeType, t2e);
  }

  @Override
  public final DefaultIBDataSource withDownloadCacheDirectory(Path cacheDir) {
    return new DefaultIBDataSource(getId(), getSourceURL(), getCredentials(), getChecksum(), getMetadata(), targetPath,
        name, description, Objects.requireNonNull(cacheDir), log, mimeType, t2e);
  }

  public DefaultIBDataSource(String id, URL source, Optional<BasicCredentials> creds, Optional<Checksum> checksum,
      Optional<Document> metadata, Log log, Optional<String> mimeType, TypeToExtensionMapper t2e) {
    this(id, source, creds, checksum, metadata, null, null, null, null, log, mimeType, t2e);
  }

  private DefaultIBDataSource(String id, URL source, Optional<BasicCredentials> creds, Optional<Checksum> checksum,
      Optional<Document> metadata, Path targetPath, String name, String description, Path cacheDir, Log log,
      Optional<String> mimeType, TypeToExtensionMapper t2e) {

    super(id, source, creds, checksum, metadata);
    this.targetPath = targetPath;
    this.name = name;
    this.description = description;
    this.cacheDirectory = cacheDir;
    this.log = log;
    this.t2e = t2e;
    this.mimeType = mimeType;

  }

  public DefaultIBDataSource(Log log, URL source, Optional<Checksum> checksum, Optional<Document> metadata, Optional<String> targetType,
      TypeToExtensionMapper t2e) {
    this(UUID.randomUUID().toString(), source, Optional.empty(), checksum, metadata, log, targetType, t2e);
  }

  @Override
  public Optional<IBChecksumPathType> get() {
    return ofNullable(targetPath).map(target -> {
      if (this.read == null) {
        switch (source.getProtocol()) {
        case "http":
        case "https":
          WGetter wget = new WGetter(log, getCredentials(), this.t2e);
          this.read = wget.collectCacheAndCopyToChecksumNamedFile(
              // Target directory
              targetPath,
              // URL
              source,
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
          try (InputStream ins = source.openStream()) {
            this.read = cet
                .withReturningTranslation(() -> copyToDeletedOnExitTempChecksumAndPath(ofNullable(targetPath),
                    IBDATA_PREFIX, IBDATA_SUFFIX, ins));

          } catch (IOException e) {
            throw new IBDataException(e);
          }
          break;
        default:
          throw new IBDataException("Default processor cannot handle protocol " + source.getProtocol());
        }
      }
      return read;
    });
  }

  @Override
  public Optional<String> getMimeType() {
    return get().map(IBChecksumPathType::getType);
  }

  @Override
  public Optional<String> getDescription() {
    return Optional.ofNullable(this.description);
  }

  @Override
  public Optional<String> getName() {
    return Optional.ofNullable(this.name);
  }
}
