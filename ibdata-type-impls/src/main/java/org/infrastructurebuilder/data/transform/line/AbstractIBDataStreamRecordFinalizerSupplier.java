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

import java.util.Objects;

import org.infrastructurebuilder.data.IBDataDataStreamRecordFinalizerSupplier;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;

abstract public class AbstractIBDataStreamRecordFinalizerSupplier<T> implements IBDataDataStreamRecordFinalizerSupplier<T> {

  private final ConfigMapSupplier cms;
  private final PathSupplier wps;

  public AbstractIBDataStreamRecordFinalizerSupplier(PathSupplier ps) {
    this(ps, null);
  }

  protected AbstractIBDataStreamRecordFinalizerSupplier(PathSupplier ps, ConfigMapSupplier cms) {
    this.wps = Objects.requireNonNull(ps);
    this.cms = cms;
  }

  protected PathSupplier getWps() {
    return wps;
  }

  protected ConfigMapSupplier getCms() {
    return cms;
  }


}