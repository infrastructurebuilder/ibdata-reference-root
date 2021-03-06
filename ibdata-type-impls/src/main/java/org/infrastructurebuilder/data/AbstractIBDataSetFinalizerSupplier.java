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

import org.infrastructurebuilder.data.ingest.AbstractIBDataConfigurableSupplier;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;
import org.slf4j.Logger;

public abstract class AbstractIBDataSetFinalizerSupplier<T>
    extends AbstractIBDataConfigurableSupplier<IBDataSetFinalizer<T>> implements IBDataSetFinalizerSupplier<T> {
  private final TypeToExtensionMapper t2e;

  protected AbstractIBDataSetFinalizerSupplier(Logger logger, PathSupplier workingPathSupplier, ConfigMapSupplier cms,
      TypeToExtensionMapper t2e) {
    super(workingPathSupplier, () -> logger, cms);
    this.t2e = t2e;
  }

  protected TypeToExtensionMapper getTypeToExtensionMapper() {
    return t2e;
  }

}
