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

import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

abstract public class AbstractIBDataSet implements IBDataSet {

  private final UUID id;
  private final Date creationDate;
  private final Object metadata;
  private final Optional<String> name;
  private final Optional<String> description;
  private final Optional<String> path;
  private final String groupId, artifactId, version;

  private Path underlyingPath = null;

  protected AbstractIBDataSet(IBDataSet set) {
    this(requireNonNull(set).getId(), set.getCreationDate(), set.getMetadata(), set.getName(), set.getDescription(),
        Optional.ofNullable(set.getPath()), set.getGroupId(), set.getArtifactId(), set.getVersion());
  }

  public AbstractIBDataSet(UUID id, Date date, Object metadata, Optional<String> name, Optional<String> description,
      Optional<String> path, String groupId, String artifactId, String version) {
    this.id = requireNonNull(id, getClass().getCanonicalName() + "." + "id");
    this.creationDate = requireNonNull(date, getClass().getCanonicalName() + "." + "creationDate");
    this.metadata = requireNonNull(metadata, getClass().getCanonicalName() + "." + "metadata");
    this.name = requireNonNull(name, getClass().getCanonicalName() + "." + "name");
    this.description = requireNonNull(description, getClass().getCanonicalName() + "." + "description");
    this.path = requireNonNull(path, getClass().getCanonicalName() + "." + "path");
    this.groupId = requireNonNull(groupId, getClass().getCanonicalName() + "." + "groupId");
    this.artifactId = requireNonNull(artifactId, getClass().getCanonicalName() + "." + "artifactId");
    this.version = requireNonNull(version, getClass().getCanonicalName() + "." + "version");
  }

  @Override
  public Date getCreationDate() {
    return this.creationDate;
  }

  @Override
  public UUID getId() {
    return this.id;
  }

  @Override
  public Optional<String> getDescription() {
    return this.description;

  }

  @Override
  public Optional<String> getName() {
    return this.name;
  }

  @Override
  public String getPath() {
    return this.path.orElse(underLyingPath().map(Path::toString).orElse(null));
  }

  @Override
  public String getGroupId() {
    return this.groupId;
  }

  @Override
  public String getArtifactId() {
    return this.artifactId;
  }

  @Override
  public String getVersion() {
    return this.version;
  }

  @Override
  public Object getMetadata() {
    return this.metadata;
  }

  protected AbstractIBDataSet setUnderlyingPath(Path p) {
    this.underlyingPath = p;
    return this;
  }

  private Optional<Path> underLyingPath() {
    return Optional.ofNullable(underlyingPath);
  }

}
