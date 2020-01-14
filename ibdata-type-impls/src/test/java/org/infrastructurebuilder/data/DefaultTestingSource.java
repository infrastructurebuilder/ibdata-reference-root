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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultTestingSource extends AbstractIBDataSource<String> {
  public final static Logger log = LoggerFactory.getLogger(DefaultTestingSource.class);
  private final static TestingPathSupplier wps = new TestingPathSupplier();

  public DefaultTestingSource(String source) {
    super(wps, log, UUID.randomUUID().toString(), source, true, empty(), empty(), empty(), empty(), empty(), empty());
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
  public Optional<String> getMimeType() {
    return empty();
  }

  @Override
  protected List<IBChecksumPathType> getInstance(PathSupplier workingPath, Optional<String> in) {
    return Collections.emptyList();
  }

}
