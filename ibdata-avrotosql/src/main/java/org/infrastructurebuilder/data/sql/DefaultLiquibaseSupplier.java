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
import static org.infrastructurebuilder.data.IBDataException.cet;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;

import org.infrastructurebuilder.data.IBDataDatabaseDriverSupplier;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBDatabaseDialectMapper;
import org.infrastructurebuilder.util.BasicCredentials;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.config.AbstractConfigurableSupplier;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;

import liquibase.Liquibase;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.ResourceAccessor;

@Named
public class DefaultLiquibaseSupplier extends AbstractConfigurableSupplier<Liquibase, ConfigMapSupplier>
    implements LiquibaseSupplier {

  public static final String SOURCE_URL = "sourceURL";
  private final PathSupplier wps;
  private final List<DataSourceSupplier> suppliers;
  private final IBDatabaseDialectMapper dialectMappper;

  @Inject
  public DefaultLiquibaseSupplier(PathSupplier wps, LoggerSupplier l, List<DataSourceSupplier> suppliers,
      IBDatabaseDialectMapper dMapper) {
    this(wps, l, suppliers, null, dMapper);
  }

  private DefaultLiquibaseSupplier(PathSupplier wps, LoggerSupplier l, List<DataSourceSupplier> suppliers,
      ConfigMapSupplier config, IBDatabaseDialectMapper dMapper) {
    super(config, l);
    this.wps = requireNonNull(wps);
    this.suppliers = requireNonNull(suppliers);
    this.dialectMappper = requireNonNull(dMapper);
  }

  public PathSupplier getWps() {
    return wps;
  }

  public List<DataSourceSupplier> getSuppliers() {
    return suppliers;
  }

  public IBDatabaseDialectMapper getDialectMappper() {
    return dialectMappper;
  }

  @Override
  public DefaultLiquibaseSupplier configure(ConfigMapSupplier config) {
    return new DefaultLiquibaseSupplier(getWps(), () -> getLog(), getSuppliers(), config, getDialectMappper());
  }

  @Override
  protected Liquibase configuredType(ConfigMapSupplier config) {
    ConfigMap c = config.get();
    String url = c.getOrDefault(SOURCE_URL, null);
    IBDataDatabaseDriverSupplier ds = getDialectMappper().getSupplierForURL(url)
        .orElseThrow(() -> new IBDataException("Failed to acquire IBDatabaseDialect for " + url));
    Optional<BasicCredentials> creds = Optional.empty(); // FIXME Where are the creds coming from?
    Supplier<DataSource> dataSource = ds.getDataSourceSupplier2(url, creds)
        .orElseThrow(() -> new IBDataException("Failed to acquire connection for " + url));
    Path physicalFile = getWps().get().resolve(UUID.randomUUID().toString() + ".xml").toAbsolutePath();
    DatabaseChangeLog changeLog = new DatabaseChangeLog(physicalFile.toString());
    List<ResourceAccessor> resourceAccessors = Arrays.asList(new ClassLoaderResourceAccessor()); // TODO get Resource accessors
    ResourceAccessor resourceAccessor = new CompositeResourceAccessor(resourceAccessors);
    Connection connection = cet.withReturningTranslation(() -> dataSource.get().getConnection());
    Database database = cet.withReturningTranslation(
        () -> DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection)));
    return new Liquibase(changeLog, resourceAccessor, database);
  }

}
