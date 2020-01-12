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

import java.nio.file.Path;

abstract public class AbstractIBSchemaSourceSupplier extends AbstractIBDataSupplier<IBSchemaSource>
    implements IBSchemaSourceSupplier {

  public AbstractIBSchemaSourceSupplier(String id, IBSchemaSource src, Path workingPath) {
    super(id, src, workingPath);
  }

  @Override
  public int compareTo(IBSchemaSourceSupplier o) {
    return get().getId().compareTo(o.get().getId());
  }


}