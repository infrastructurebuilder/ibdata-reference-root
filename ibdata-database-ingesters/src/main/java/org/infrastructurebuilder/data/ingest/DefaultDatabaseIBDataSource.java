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

import java.net.URL;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.infrastructurebuilder.data.AbstractIBDataSource;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.util.BasicCredentials;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.w3c.dom.Document;

public class DefaultDatabaseIBDataSource extends AbstractIBDataSource implements AutoCloseable {
  public static final String QUERY = "query";

  private final Path targetPath;
  private final String name; // FIXME Move name, description, cacheDirectory into AbstractIBDataSource
  private final String description;
  private final Path cacheDirectory;
  private final Logger log;
  private final TypeToExtensionMapper t2e;
  private final Optional<String> mimeType;

  private IBChecksumPathType read;

  @Override
  public final DefaultDatabaseIBDataSource withTargetPath(Path targetPath) {
    return new DefaultDatabaseIBDataSource(getId(), getSourceURL(), getCredentials(), getChecksum(), getMetadata(),
        getAdditionalConfig(), Objects.requireNonNull(targetPath), name, description, cacheDirectory, log, mimeType,
        t2e);
  }

  @Override
  public final DefaultDatabaseIBDataSource withName(String name) {
    return new DefaultDatabaseIBDataSource(getId(), getSourceURL(), getCredentials(), getChecksum(), getMetadata(),
        getAdditionalConfig(), targetPath, Objects.requireNonNull(name), description, cacheDirectory, log, mimeType,
        t2e);
  }

  @Override
  public final DefaultDatabaseIBDataSource withDescription(String description) {
    return new DefaultDatabaseIBDataSource(getId(), getSourceURL(), getCredentials(), getChecksum(), getMetadata(),
        getAdditionalConfig(), targetPath, name, Objects.requireNonNull(description), cacheDirectory, log, mimeType,
        t2e);
  }

  @Override
  public final DefaultDatabaseIBDataSource withDownloadCacheDirectory(Path cacheDir) {
    return new DefaultDatabaseIBDataSource(getId(), getSourceURL(), getCredentials(), getChecksum(), getMetadata(),
        getAdditionalConfig(), targetPath, name, description, Objects.requireNonNull(cacheDir), log, mimeType, t2e);
  }

  public DefaultDatabaseIBDataSource(String id, URL source, Optional<BasicCredentials> creds,
      Optional<Checksum> checksum, Optional<Document> metadata, Logger log, Optional<String> mimeType,
      TypeToExtensionMapper t2e) {
    this(id, source, creds, checksum, metadata, Optional.empty(), null, null, null, null, log, mimeType, t2e);
  }

  private DefaultDatabaseIBDataSource(String id, URL source, Optional<BasicCredentials> creds,
      Optional<Checksum> checksum, Optional<Document> metadata, Optional<ConfigMap> additionalConfig, Path targetPath,
      String name, String description, Path cacheDir, Logger log, Optional<String> mimeType,
      TypeToExtensionMapper t2e) {

    super(id, source, creds, checksum, metadata, additionalConfig);
    this.targetPath = targetPath;
    this.name = name;
    this.description = description;
    this.cacheDirectory = cacheDir;
    this.log = log;
    this.t2e = t2e;
    this.mimeType = mimeType;

  }

  private Connection conn;
  private DSLContext create;

  @Override
  public void close() throws Exception {
    conn.close();
  }

  public DefaultDatabaseIBDataSource(Logger log, URL source, Optional<Checksum> checksum, Optional<Document> metadata,
      Optional<String> targetType, TypeToExtensionMapper t2e) {
    this(UUID.randomUUID().toString(), source, Optional.empty(), checksum, metadata, log, targetType, t2e);
  }

  @Override
  public Optional<IBChecksumPathType> get() {
    String sql = getAdditionalConfig().orElseThrow(() -> new IBDataException("No additional config available (query)")).getString(QUERY);
    return ofNullable(getSourceURL()).map(source -> {

      if (this.read == null) {
        switch (source.getProtocol()) {
        case "jdbc":

          DSLContext create = DSL.using(conn, SQLDialect.MYSQL);
          Result<Record> result = create.fetch(sql);

          //          WGetter wget = new WGetter(log, getCredentials(), this.t2e);
          //          this.read = wget.collectCacheAndCopyToChecksumNamedFile(
          //              // Target directory
          //              targetPath,
          //              // URL
          //              source,
          //              // "Optional" checksum
          //              checksum,
          //              // Location of the cacheDirectory
          //              cacheDirectory,
          //              // Mime type from supplied value (not calculated value)
          //              this.mimeType,
          //              // Slightly insane defaults
          //              false, 3, 0, false).orElseThrow(() -> new IBDataException("Could not read "));
          break;
        default:
          throw new IBDataException("Processor " + getId() + " cannot handle protocol " + source.getProtocol());
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
