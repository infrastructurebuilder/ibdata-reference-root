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
import java.util.Objects;

import org.infrastructurebuilder.data.IBDataTransformer;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;
import org.slf4j.Logger;

abstract public class AbstractIBDataTransformer implements IBDataTransformer {

  private final IBRuntimeUtils ibr;
  private final ConfigMap config;

  public AbstractIBDataTransformer(IBRuntimeUtils p) {
    this(p, new ConfigMap());
  }

  protected AbstractIBDataTransformer(IBRuntimeUtils workingPath, ConfigMap config) {
    this.ibr = Objects.requireNonNull(workingPath);
    this.config = config;
  }

  protected ConfigMap getConfig() {
    return config;
  }

  public Path getWorkingPath() {
    return ibr.getWorkingPath();
  }

  @Override
  public Logger getLog() {
    return ibr.getLog();
  }
}
