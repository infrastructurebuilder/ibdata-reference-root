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
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.config.AbstractConfigurableSupplier;
import org.infrastructurebuilder.util.config.ConfigMap;

@Named
public class DefaultGenericDataSupplier extends AbstractConfigurableSupplier<GenericData, ConfigMap>
    implements GenericDataSupplier {

  @Inject
  public DefaultGenericDataSupplier(LoggerSupplier l) {
    super(null, l);
  }

  private DefaultGenericDataSupplier(ConfigMap config, LoggerSupplier l) {
    super(config, l);
  }

  @Override
  public GenericDataSupplier configure(ConfigMap config) {
    return new DefaultGenericDataSupplier(config, () -> getLog());
  }

  @Override
  protected GenericData getInstance() {
    Formatters f = new Formatters(getConfig());
    return new MapProxyGenericData(f);
  }

}
