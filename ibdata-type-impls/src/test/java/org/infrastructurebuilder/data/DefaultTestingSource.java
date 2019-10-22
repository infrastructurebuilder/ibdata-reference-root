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

import static java.util.Optional.empty;

import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import org.infrastructurebuilder.util.files.IBChecksumPathType;

public class DefaultTestingSource extends AbstractIBDataSource implements IBDataSource {

  public DefaultTestingSource(URL source) {
    super(UUID.randomUUID().toString(), source, empty(), empty(), empty());
  }

  @Override
  public Optional<IBChecksumPathType> get() {
    return empty();
  }

  @Override
  public Optional<String> getName() {
    return empty();
  }

  @Override
  public Optional<String> getDescription() {
    return empty();
  }

  @Override
  public IBDataSource withTargetPath(Path targetPath) {
    return this;
  }

  @Override
  public IBDataSource withName(String name) {
    return this;
  }

  @Override
  public IBDataSource withDescription(String description) {
    return this;
  }

  @Override
  public IBDataSource withDownloadCacheDirectory(Path cacheDir) {
    return this;
  }

}
