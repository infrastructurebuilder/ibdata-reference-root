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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.infrastructurebuilder.data.model.DataSet;

abstract public class AbstractIBDataSet extends DataSet implements IBDataSet {
  private static final long serialVersionUID = 3647886656035674148L;
  private final Optional<String> path;

  protected AbstractIBDataSet(IBDataSet set) {
    this(requireNonNull(set).getId(), set.getCreationDate(), set.getMetadata(), set.getName(), set.getDescription(),
        Optional.ofNullable(set.getPath()), set.getGroupId(), set.getArtifactId(), set.getVersion());
  }

  public AbstractIBDataSet(UUID id, Date date, Object metadata, Optional<String> name, Optional<String> description,
      Optional<String> path, String groupId, String artifactId, String version) {
    super();
    setCreationDate(requireNonNull(date, getClass().getCanonicalName() + "." + "creationDate"));
    setDataSetDescription(
        requireNonNull(description, getClass().getCanonicalName() + "." + "description").orElse(null));
    setDataSetName(requireNonNull(name.orElse(null), getClass().getCanonicalName() + "." + "name"));
    setMetadata(requireNonNull(metadata, getClass().getCanonicalName() + "." + "metadata"));
    setGroupId(requireNonNull(groupId, getClass().getCanonicalName() + "." + "groupId"));
    setArtifactId(requireNonNull(artifactId, getClass().getCanonicalName() + "." + "artifactId"));
    setVersion(requireNonNull(version, getClass().getCanonicalName() + "." + "version"));
    setUuid(requireNonNull(id, getClass().getCanonicalName() + "." + "id").toString());
    this.path = requireNonNull(path, getClass().getCanonicalName() + "." + "path");
  }

  @Override
  public String getPath() {
    return this.path.orElse(underLyingPath().map(Path::toString).orElse(null));
  }

  @Override
  public List<IBSchema> getSchemaSuppliers() {
    return getSchemas().stream().collect(Collectors.toList());
  }

}
