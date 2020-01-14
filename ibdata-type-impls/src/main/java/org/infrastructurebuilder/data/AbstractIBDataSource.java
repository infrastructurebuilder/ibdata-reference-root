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
package org.infrastructurebuilder.data;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;

import org.infrastructurebuilder.util.BasicCredentials;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.AbstractConfigurableSupplier;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.slf4j.Logger;

abstract public class AbstractIBDataSource<P> extends AbstractConfigurableSupplier<List<IBChecksumPathType>, ConfigMap,P>
    implements IBDataSource<P> {

  protected final String id;
  protected final String source;
  protected final Optional<BasicCredentials> creds;
  protected final Optional<Checksum> checksum;
  protected final Optional<Metadata> metadata;
  protected final Optional<String> name;
  protected final Optional<String> desc;
  private final boolean expandArchives;

  public AbstractIBDataSource(
      // Wprlomg target
      PathSupplier wps,
      // Logger
      Logger logger
  // Temp id
      , String id
      // "URL" or JDBC URL etc
      , String source
      // True if we expand archives
      , boolean expand
      // Name
      , Optional<String> name
      // Description
      , Optional<String> desc
      // Creds
      , Optional<BasicCredentials> creds
      // Target value checksum
      , Optional<Checksum> checksum
      // Metdata
      , Optional<Metadata> metadata
      // Configuration
      , Optional<ConfigMap> config) {
    super(wps, config.orElse(null), () -> logger);
    this.id = requireNonNull(id);
    this.source = requireNonNull(source);
    this.creds = requireNonNull(creds);
    this.checksum = requireNonNull(checksum);
    this.metadata = requireNonNull(metadata);
    this.name = requireNonNull(name);
    this.desc = requireNonNull(desc);
    this.expandArchives = expand;
  }

  @Override
  public String getSourceURL() {
    return source;
  }

  @Override
  public Optional<BasicCredentials> getCredentials() {
    return creds;
  }

  @Override
  public Optional<Checksum> getChecksum() {
    return checksum;
  }

  @Override
  public Optional<Metadata> getMetadata() {
    return metadata;
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public Optional<String> getName() {
    return this.name;
  }

  @Override
  public Optional<String> getDescription() {
    return this.desc;
  }

  @Override
  public boolean isExpandArchives() {
    return this.expandArchives;
  }

  /**
   * Override this to acquire additional configuration  OR ELSE IT NEVER HAPPENED!
   */
  @Override
  public IBDataSource configure(ConfigMap config) {
    return this;
  }
}