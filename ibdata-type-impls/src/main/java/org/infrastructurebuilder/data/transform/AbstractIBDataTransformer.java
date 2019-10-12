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
package org.infrastructurebuilder.data.transform;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.infrastructurebuilder.data.IBDataTransformer;

abstract public class AbstractIBDataTransformer implements IBDataTransformer {

  private final Path workingPath;
  private final Map<String, String> config;

  public AbstractIBDataTransformer(Path p) {
    this(p, new HashMap<>());
  }

  protected AbstractIBDataTransformer(Path workingPath, Map<String, String> config) {
    this.workingPath = Objects.requireNonNull(workingPath);
    this.config = config;
  }

  protected Map<String, String> getConfig() {
    return config;
  }

  protected Path getWorkingPath() {
    return workingPath;
  }

}
