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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

abstract public class AbstractIBDataSetFinalizer<T> implements IBDataSetFinalizer<T> {

  private final Map<String, String> config;
  private final Path   workingPath;

  protected AbstractIBDataSetFinalizer(Map<String, String> map, Path workingPath) {
    this.config = Objects.requireNonNull(map);
    this.workingPath = Objects.requireNonNull(workingPath);
    try {
      Files.createDirectories(this.workingPath);
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }

  protected Map<String, String> getConfig() {
    return config;
  }

  @Override
  public Path getWorkingPath() {
    return workingPath;
  }


}
