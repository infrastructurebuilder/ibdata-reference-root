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

import static org.infrastructurebuilder.data.IBDataException.cet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.infrastructurebuilder.util.config.ConfigMap;

abstract public class AbstractIBDataSetFinalizer<T> implements IBDataSetFinalizer<T> {

  private final ConfigMap config;
  private final Path workingPath;

  protected AbstractIBDataSetFinalizer(ConfigMap map, Path workingPath) {
    this.config = Objects.requireNonNull(map);
    this.workingPath = Objects.requireNonNull(workingPath);
    cet.withReturningTranslation(() -> Files.createDirectories(this.workingPath));
  }

  protected ConfigMap getConfig() {
    return config;
  }

  @Override
  public Path getWorkingPath() {
    return workingPath;
  }

}
