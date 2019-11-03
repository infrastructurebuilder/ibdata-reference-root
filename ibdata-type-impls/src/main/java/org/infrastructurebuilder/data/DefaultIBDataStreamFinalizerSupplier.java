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
import static org.infrastructurebuilder.data.IBDataConstants.WORKING_PATH_CONFIG_ITEM;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;

@Named
public class DefaultIBDataStreamFinalizerSupplier extends AbstractIBDataStreamFinalizerSupplier {

  private static final String WP_MESSAGE = "Working Path Config";

  @Inject
  public DefaultIBDataStreamFinalizerSupplier(LoggerSupplier l) {
    this(null, null, l);
  }

  /**
   * @param wps
   * @param cms
   */
  protected DefaultIBDataStreamFinalizerSupplier(PathSupplier wps, ConfigMapSupplier cms, LoggerSupplier l) {
    super(wps, cms, l);
  }

  @Override
  public IBDataStreamFinalizerSupplier configure(ConfigMapSupplier cms) {
    return new DefaultIBDataStreamFinalizerSupplier(
        () -> Paths.get(requireNonNull(requireNonNull(cms).get().getString(WORKING_PATH_CONFIG_ITEM), WP_MESSAGE)), cms,
        () -> getLog());
  }

  @Override
  public IBDataStreamFinalizer get() {
    return new DefaultIBDataStreamFinalizer(getWps().get(), getCms().get());
  }

  private class DefaultIBDataStreamFinalizer extends AbstractIBDataStreamFinalizer {

    DefaultIBDataStreamFinalizer(Path path, ConfigMap map) {
      super(path, map);
    }
  }

}
