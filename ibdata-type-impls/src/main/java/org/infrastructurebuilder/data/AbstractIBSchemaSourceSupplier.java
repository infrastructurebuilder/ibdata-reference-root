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

import static java.util.Objects.requireNonNull;

import org.infrastructurebuilder.data.ingest.IBDataSchemaIngestionConfig;
import org.infrastructurebuilder.util.config.PathSupplier;

abstract public class AbstractIBSchemaSourceSupplier<P> extends AbstractIBDataSupplier<IBSchemaSource<P>>
    implements IBSchemaSourceSupplier {
  private final IBDataSchemaIngestionConfig ibdsic;

  public AbstractIBSchemaSourceSupplier(String id, IBSchemaSource<P> src, PathSupplier workingPath, IBDataSchemaIngestionConfig cfg) {
    super(id, src, workingPath);
    this.ibdsic = requireNonNull(cfg);

  }

  @Override
  public int compareTo(IBSchemaSourceSupplier o) {
    return get().getId().compareTo(o.get().getId());
  }

  @Override
  public IBDataSchemaIngestionConfig getIngestionConfig() {
    return ibdsic;
  }

  @Override
  public void close() throws Exception {
  }


}