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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.infrastructurebuilder.util.config.CMSConfigurableSupplier;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.Configurable;
import org.infrastructurebuilder.util.files.IBChecksumPathType;

/**
 * Returns a writen IBDataSet from the supplied finalizer config type
 * @author mykel.alvis
 *
 * @param <T>
 */
public interface IBDataSetFinalizer<T>  {

  IBChecksumPathType finalize(IBDataSet dsi1, T target, List<IBDataStreamSupplier> suppliers) throws IOException;

  Path getWorkingPath();

}
