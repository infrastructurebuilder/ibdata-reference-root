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

import static java.util.Objects.requireNonNull;

import org.infrastructurebuilder.data.IBDataDataStreamRecordFinalizerSupplier;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.slf4j.Logger;

abstract public class AbstractIBDataStreamRecordFinalizerSupplier<T>
    implements IBDataDataStreamRecordFinalizerSupplier<T> {

  private final ConfigMapSupplier cms;
  private final IBRuntimeUtils ibr;

  public AbstractIBDataStreamRecordFinalizerSupplier(IBRuntimeUtils ibr) {
    this(ibr, null);
  }

  protected AbstractIBDataStreamRecordFinalizerSupplier(IBRuntimeUtils ibr, ConfigMapSupplier cms) {
    this.ibr = requireNonNull(ibr);
    this.cms = cms;
  }

  public Logger getLog() {
    return ibr.getLog();
  }

  protected PathSupplier getWorkingPathSupplier() {
    return () -> ibr.getWorkingPath();
  }

  protected ConfigMapSupplier getCms() {
    return cms;
  }

  public IBRuntimeUtils getRuntimeUtils() {
    return ibr;
  }

}