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
package org.infrastructurebuilder.data.sql;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.ConfigurableSupplier;

import liquibase.Liquibase;

@Named
public class DefaultLiquibaseSupplier extends AbstractConfigurableSupplier<Liquibase, ConfigMapSupplier>
    implements LiquibaseSupplier {

  @Inject
  public DefaultLiquibaseSupplier() {
    super(null);
  }

  private DefaultLiquibaseSupplier(ConfigMapSupplier config) {
    super(config);
  }

  @Override
  public ConfigurableSupplier<Liquibase, ConfigMapSupplier> configure(ConfigMapSupplier config) {
    return new DefaultLiquibaseSupplier(config);
  }

  @Override
  protected Liquibase configuredType(ConfigMapSupplier config) {
    ConfigMap c = config.get();
    // TODO Auto-generated method stub
    return null;
  }

}
