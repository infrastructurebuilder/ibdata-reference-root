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
import org.infrastructurebuilder.util.URLAndCreds;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.AbstractConfigurableSupplier;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.IBResource;
import org.slf4j.Logger;

abstract public class AbstractIBDataSource<P> extends AbstractConfigurableSupplier<List<IBResource>, ConfigMap, P>
    implements IBDataSource<P> {

  protected final String id;
  protected final URLAndCreds source;
  protected final Optional<Checksum> checksum;
  protected final Optional<Metadata> metadata;
  protected final Optional<String> namespace;
  protected final Optional<String> name;
  protected final Optional<String> desc;
  private final boolean expandArchives;

  public AbstractIBDataSource(
      // PathSupplier target
      PathSupplier wps,
      // Logger
      Logger logger
      // Temp id
      , String id
      // "URL" or JDBC URL etc
      , URLAndCreds source
      // True if we expand archives
      , boolean expand
      // Namespace
      , Optional<String> namespace
      // Name
      , Optional<String> name
      // Description
      , Optional<String> desc
      // Target value checksum
      , Optional<Checksum> checksum
      // Metdata
      , Optional<Metadata> metadata
      // Configuration
      , Optional<ConfigMap> config
      // Parameter
      , P param) {
    super(wps, config.orElse(null), () -> logger, param);
    this.id = requireNonNull(id);
    this.source = requireNonNull(source);
    this.checksum = requireNonNull(checksum);
    this.metadata = requireNonNull(metadata);
    this.namespace = requireNonNull(namespace);
    this.name = requireNonNull(name);
    this.desc = requireNonNull(desc);
    this.expandArchives = expand;
  }

  @Override
  public URLAndCreds getSource() {
    return source;
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
  public Optional<String> getNamespace() {
    return this.namespace;
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
   * Override this to acquire additional configuration OR ELSE IT NEVER HAPPENED!
   */
  @Override
  public IBDataSource<P> configure(ConfigMap config) {
    return this;
  }
}