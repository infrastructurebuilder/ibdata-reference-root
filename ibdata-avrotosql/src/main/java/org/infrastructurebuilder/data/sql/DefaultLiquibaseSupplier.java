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

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.config.AbstractConfigurableSupplier;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.ConfigurableSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.slf4j.Logger;
import org.sqlite.jdbc4.JDBC4Connection;

import liquibase.Liquibase;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.ResourceAccessor;

@Named
public class DefaultLiquibaseSupplier extends AbstractConfigurableSupplier<Liquibase, ConfigMapSupplier>
    implements LiquibaseSupplier {

  private final PathSupplier wps;
  private final Logger log;
  private final List<DataSourceSupplier> suppliers;

  @Inject
  public DefaultLiquibaseSupplier(PathSupplier wps, LoggerSupplier l, List<DataSourceSupplier> suppliers) {
    this(wps, l, suppliers, null);
  }

  private DefaultLiquibaseSupplier(PathSupplier wps, LoggerSupplier l, List<DataSourceSupplier> suppliers,
      ConfigMapSupplier config) {
    super(config);
    this.wps = requireNonNull(wps);
    this.log = requireNonNull(l).get();
    this.suppliers = requireNonNull(suppliers);
  }

  public PathSupplier getWps() {
    return wps;
  }

  public Logger getLog() {
    return log;
  }

  public List<DataSourceSupplier> getSuppliers() {
    return suppliers;
  }

  @Override
  public DefaultLiquibaseSupplier configure(ConfigMapSupplier config) {
    return new DefaultLiquibaseSupplier(getWps(), () -> getLog(), getSuppliers(), config);
  }

  @Override
  protected Liquibase configuredType(ConfigMapSupplier config) {
    ConfigMap c = config.get();
    String url = c.getOrDefault("sourceURL", null);
    Path physicalFile = getWps().get().resolve(UUID.randomUUID().toString() + ".xml").toAbsolutePath();
    DatabaseChangeLog changeLog = new DatabaseChangeLog(physicalFile.toString());
    List<ResourceAccessor> resourceAccessors = new ArrayList<>(); // TODO get Resource accessors
    ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();// new CompositeResourceAccessor(resourceAccessors);
    Connection connection  = null;
    Database database = IBDataException.cet.withReturningTranslation(() -> DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection)));
    return new Liquibase(changeLog, resourceAccessor, database);
  }

}
