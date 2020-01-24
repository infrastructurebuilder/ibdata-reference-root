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
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;
import static org.infrastructurebuilder.data.IBDataException.cet;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;

import org.infrastructurebuilder.data.DefaultURLAndCreds;
import org.infrastructurebuilder.data.IBDataDatabaseDriverSupplier;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBDatabaseDialectMapper;
import org.infrastructurebuilder.data.URLAndCreds;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.config.AbstractCMSConfigurableSupplier;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.slf4j.Logger;

import liquibase.Liquibase;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;

@Named
public class DefaultLiquibaseSupplier extends AbstractCMSConfigurableSupplier<Liquibase, URLAndCreds>
    implements LiquibaseSupplier {

  private final List<IBDataDatabaseDriverSupplier> suppliers;
  private final IBDatabaseDialectMapper dialectMappper;

  @Inject
  public DefaultLiquibaseSupplier(@Named(IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps, LoggerSupplier l,
      List<IBDataDatabaseDriverSupplier> suppliers, IBDatabaseDialectMapper dMapper) {
    super(wps, null, l, null);
    this.suppliers = requireNonNull(suppliers);
    this.dialectMappper = requireNonNull(dMapper);
  }

  private DefaultLiquibaseSupplier(PathSupplier wps, LoggerSupplier l, List<IBDataDatabaseDriverSupplier> suppliers,
      ConfigMapSupplier config, IBDatabaseDialectMapper dMapper, URLAndCreds urlAndCreds) {
    super(wps, config, l, requireNonNull(urlAndCreds));
    this.suppliers = requireNonNull(suppliers);
    this.dialectMappper = requireNonNull(dMapper);
  }

  public List<IBDataDatabaseDriverSupplier> getSuppliers() {
    return suppliers;
  }

  public IBDatabaseDialectMapper getDialectMapper() {
    return dialectMappper;
  }

  @Override
  public AbstractCMSConfigurableSupplier<Liquibase, URLAndCreds> getConfiguredSupplier(ConfigMapSupplier cms) {
    return new DefaultLiquibaseSupplier(getWorkingPathSupplier(), () -> getLog(), getSuppliers(), cms,
        getDialectMapper(), new DefaultURLAndCreds(requireNonNull(cms).get()));
  }

  @Override
  protected Liquibase getInstance(PathSupplier workingPath, URLAndCreds in) {
    IBDataDatabaseDriverSupplier ds = getDialectMapper().getSupplierForURL(in.getUrl())
        .orElseThrow(() -> new IBDataException("Failed to acquire IBDatabaseDialect for " + in.getUrl()));
    DataSource dataSource = ds.getDataSourceSupplier(in)
        .orElseThrow(() -> new IBDataException("Failed to acquire DataSource for " + in.getUrl())).get();
    Path physicalFile = workingPath.get().resolve(UUID.randomUUID().toString() + ".xml").toAbsolutePath();
    DatabaseChangeLog changeLog = new DatabaseChangeLog(physicalFile.toString());
    // TODO get Resource accessors
    ResourceAccessor resourceAccessor = new CompositeResourceAccessor(Arrays.asList(new ClassLoaderResourceAccessor()));
    Connection connection = cet.withReturningTranslation(() -> dataSource.getConnection());
    Database database = cet.withReturningTranslation(
        () -> DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection)));
    return new Liquibase(changeLog, resourceAccessor, database);
  }

//  public final ResourceAccessor createResourceOpener(ClassLoader cl, List<Path> additionalPaths) {
//    List<ResourceAccessor> l = new ArrayList<>();
//    l.addAll(Objects.requireNonNull(additionalPaths).stream().map(Path::toAbsolutePath).map(Path::toString)
//        .map(FileSystemResourceAccessor::new).collect(Collectors.toList()));
//    l.add(new ClassLoaderResourceAccessor(Objects.requireNonNull(cl)));
//    return new CompositeResourceAccessor(l);
//  }
//
//  public final ResourceAccessor createResourceOpener(ClassLoader cl) {
//    return createResourceOpener(cl, Collections.emptyList());
//  }
//
//  public final Liquibase createLiquibase(Connection c, String changelog, ConfigMapSupplier cms, boolean dropFirst)
//      throws LiquibaseException {
//    ResourceAccessor resourceAccessor = createResourceOpener(c.getClass().getClassLoader());
//    Liquibase liquibase = ls.get();
//    liquibase.setIgnoreClasspathPrefix(true);
//    ConfigMap parametersSupplier = cms.get();
//    if (parametersSupplier != null) {
//      for (String entry : parametersSupplier.keySet()) {
//        liquibase.setChangeLogParameter(entry, parametersSupplier.getString(entry));
//      }
//    }
//
//    if (dropFirst) {
//      liquibase.dropAll();
//    }
//
//    return liquibase;
//  }

}
