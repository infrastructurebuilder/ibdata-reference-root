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

import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.slf4j.Logger;

public abstract class AbstractIBDataStreamFinalizerSupplier implements IBDataStreamFinalizerSupplier {

  private final PathSupplier wps;
  private final ConfigMapSupplier cms;
  private final Logger logger;

  protected AbstractIBDataStreamFinalizerSupplier(PathSupplier wps, ConfigMapSupplier cms, LoggerSupplier l) {
    this.wps = wps;
    this.cms = cms;
    this.logger = l.get();
  }

  protected ConfigMapSupplier getCms() {
    return cms;
  }

  protected PathSupplier getWps() {
    return wps;
  }

  @Override
  public Logger getLog() {
    return this.logger;
  }
}
