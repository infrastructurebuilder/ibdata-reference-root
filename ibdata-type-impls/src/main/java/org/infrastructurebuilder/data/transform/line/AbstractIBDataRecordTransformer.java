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
package org.infrastructurebuilder.data.transform.line;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.infrastructurebuilder.util.config.ConfigMap;

abstract public class AbstractIBDataRecordTransformer<I, O> implements IBDataRecordTransformer<I, O> {

  private final Path workingPath;
  private final ConfigMap config;

  public AbstractIBDataRecordTransformer(Path ps) {
    this(ps, null);
  }

  protected AbstractIBDataRecordTransformer(Path ps, ConfigMap config) {
    this.workingPath = Objects.requireNonNull(ps);
    this.config = config;
  }

  protected Path getWorkingPath() {
    return workingPath;
  }

  protected ConfigMap getConfig() {
    return config;
  }

  protected String getConfiguration(String key) {
    return getConfig().get(getHint() + "." + key);
  }

  protected String getConfiguration(String key, String defaultValue) {
    return getConfig().getOrDefault(getHint() + "." + key, defaultValue);
  }

  @SuppressWarnings("unchecked")
  protected Optional<I> getTypedObject(Object in) {
    try {
      return Optional.ofNullable((I) in);
    } catch (ClassCastException e) {
      return Optional.empty();
    }
  }

  @Override
  public boolean respondsTo(Object o) {
    return getTypedObject(o).isPresent();
  }

}