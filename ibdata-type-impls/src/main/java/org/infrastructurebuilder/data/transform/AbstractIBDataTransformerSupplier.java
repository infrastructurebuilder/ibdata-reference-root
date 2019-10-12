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

import javax.inject.Named;

import org.infrastructurebuilder.data.IBDataStreamRecordFinalizer;
import org.infrastructurebuilder.data.IBDataTransformer;
import org.infrastructurebuilder.data.IBDataTransformerSupplier;
import org.infrastructurebuilder.data.IBMetadataUtils;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;

abstract public class AbstractIBDataTransformerSupplier<T> implements IBDataTransformerSupplier<T> {
  private final PathSupplier wps;
  private final ConfigMapSupplier config;

  public AbstractIBDataTransformerSupplier(
      // These go into your impls
      @Named(IBMetadataUtils.IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps) {
    this(wps, null);
  }

  protected AbstractIBDataTransformerSupplier(
      // These go into your impls
      @Named(IBMetadataUtils.IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps,
      ConfigMapSupplier cms) {
    this.wps = Objects.requireNonNull(wps);
    this.config = cms;
  }

  public PathSupplier getWps() {
    return wps;
  }

  @Override
  public abstract IBDataTransformerSupplier<T> configure(ConfigMapSupplier cms);

  @Override
  public IBDataTransformerSupplier<T> withFinalizer(IBDataStreamRecordFinalizer<?> ts2) {
    return this;
  }

  @Override
  public IBDataTransformer get() {
    return getUnconfiguredTransformerInstance(this.wps.get()).configure(this.config.get());
  }

  /**
   * Must return a new instance of an IBDataTransformer
   * @param workingPath
   * @return
   */
  protected abstract IBDataTransformer getUnconfiguredTransformerInstance(Path workingPath);

}
