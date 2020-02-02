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

import org.infrastructurebuilder.data.IBDataStreamRecordFinalizer;
import org.infrastructurebuilder.data.IBDataTransformer;
import org.infrastructurebuilder.data.IBDataTransformerSupplier;
import org.infrastructurebuilder.util.LoggerEnabled;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.slf4j.Logger;

abstract public class AbstractIBDataTransformerSupplier implements IBDataTransformerSupplier, LoggerEnabled {
  private final IBRuntimeUtils ibr;
  private final ConfigMapSupplier config;

  public AbstractIBDataTransformerSupplier(IBRuntimeUtils ibr) {
    this(ibr, null);
  }

  protected AbstractIBDataTransformerSupplier(
      // These go into your impls
      IBRuntimeUtils ibr, ConfigMapSupplier cms) {
    this.ibr = Objects.requireNonNull(ibr);
    this.config = cms;
  }

  public PathSupplier getWorkingPathSupplier() {
    return () -> ibr.getWorkingPath();
  }

  @Override
  public Logger getLog() {
    return this.ibr.getLog();
  }

  public ConfigMap getConfig() {
    return config.get();
  }

  @Override
  public abstract IBDataTransformerSupplier configure(ConfigMapSupplier cms);

  @Override
  public IBDataTransformerSupplier withFinalizer(IBDataStreamRecordFinalizer<?> ts2) {
    return this;
  }

  @Override
  public IBDataTransformer get() {
    return getConfiguredTransformerInstance();
  }

  public IBRuntimeUtils getRuntimeUtils() {
    return ibr;
  }
  /**
   * Must return a new instance of an IBDataTransformer
   *
   * @param workingPath
   * @return
   */
  protected abstract IBDataTransformer getConfiguredTransformerInstance();

}
