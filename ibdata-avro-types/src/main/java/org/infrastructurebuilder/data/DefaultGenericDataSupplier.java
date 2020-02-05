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

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.MapProxyGenericData;
import org.infrastructurebuilder.util.config.AbstractCMSConfigurableSupplier;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;

@Named
public class DefaultGenericDataSupplier extends AbstractCMSConfigurableSupplier<GenericData, Formatters>
    implements GenericDataSupplier {

  @Inject
  public DefaultGenericDataSupplier(IBRuntimeUtils ibr) {
    super(ibr, null, null);
  }

  private DefaultGenericDataSupplier(IBRuntimeUtils ibr, ConfigMapSupplier config, Formatters f) {
    super(ibr, config, f);
  }

  @Override
  public DefaultGenericDataSupplier getConfiguredSupplier(ConfigMapSupplier config) {
    return new DefaultGenericDataSupplier(getRuntimeUtils(), config, new Formatters(config.get()));
  }

  @Override
  protected GenericData getInstance(IBRuntimeUtils ibr, Formatters f) {
    return new MapProxyGenericData(f);
  }

}
