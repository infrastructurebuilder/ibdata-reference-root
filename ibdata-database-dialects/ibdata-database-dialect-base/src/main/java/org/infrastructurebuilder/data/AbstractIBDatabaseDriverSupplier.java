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

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.infrastructurebuilder.data.IBDataException.cet;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.infrastructurebuilder.util.BasicCredentials;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.artifacts.GAV;
import org.infrastructurebuilder.util.artifacts.impl.DefaultGAV;
import org.jooq.SQLDialect;
import org.jooq.SQLDialect.ThirdParty;
import org.slf4j.Logger;

import liquibase.database.Database;

abstract public class AbstractIBDatabaseDriverSupplier
    //extends    AbstractConfigurableSupplier<IBDataDatabaseDriverSupplier, ClassLoader>
    implements IBDataDatabaseDriverSupplier {

  private final String hint;
  private final String databaseClass;
  private final List<DefaultGAV> gavs;
  private final Database database;
  private final Logger log;

  protected AbstractIBDatabaseDriverSupplier(LoggerSupplier l, String hint, String liquibaseDatabaseClass,
      String... list) {
    this.log = l.get();
    this.hint = requireNonNull(hint);
    this.databaseClass = requireNonNull(liquibaseDatabaseClass);
    this.database = (Database) cet.withReturningTranslation(() -> Class.forName(databaseClass).newInstance());
    this.gavs = requireNonNull(asList(list).stream().map(DefaultGAV::new).collect(toList()));
  }

  @Override
  public Optional<IBDatabaseDialect> getDialect(String jdbcUrl) {
    return getDatabaseDriverClassName(jdbcUrl).flatMap(driver -> IBDataDatabaseUtils.bySQLDialectName(getJooqName())
        .map(j -> new DefaultIBDatabaseDialect(j, this.databaseClass)));
  }

  private final Database getDatabase() {
    return this.database;
  }

  public List<GAV> getRequiredArtifacts() {
    return unmodifiableList(this.gavs);
  }

  @Override
  public Optional<Supplier<Connection>> getConnectionSupplier(String jdbcURL, Optional<BasicCredentials> creds) {
    return getDatabaseDriverClassName(jdbcURL).map(driverClass -> {
      cet.withTranslation(() -> Class.forName(driverClass));
      return () -> creds.map(cr -> {
        return cet.withReturningTranslation(
            () -> DriverManager.getConnection(jdbcURL, cr.getKeyId(), cr.getSecret().orElse(null)));
      }).orElse(cet.withReturningTranslation(() -> DriverManager.getConnection(jdbcURL)));
    });
  }

  @Override
  public String getHint() {
    return hint;
  }

  @Override
  public Optional<String> getDatabaseDriverClassName(String jdbcUrl) {
    return ofNullable(jdbcUrl)
        .flatMap(url -> ofNullable(ofNullable(getDatabase()).map(db -> db.getDefaultDriver(url)).orElse(null)));
  }

  @Override
  public boolean respondsTo(String jdbcURL) {
    return getDatabaseDriverClassName(jdbcURL).isPresent();
  }

  private final class DefaultIBDatabaseDialect implements IBDatabaseDialect {

    private final SQLDialect jd;
    private final String liquibase;
    private final Optional<ThirdParty> thirdParty;

    public DefaultIBDatabaseDialect(SQLDialect d, String liquibaseDatabaseClass) {
      this.jd = requireNonNull(d);
      this.thirdParty = ofNullable(jd.thirdParty());
      this.liquibase = requireNonNull(liquibaseDatabaseClass);
    }

    @Override
    public String get() {
      return this.jd.getName();
    }

    @Override
    public Optional<String> hibernateDialectClass() {
      return this.thirdParty.map(ThirdParty::hibernateDialect);
    }

    @Override
    public String liquibaseDatabaseClass() {
      return this.liquibase;
    }

    @Override
    public Optional<String> springDbName() {
      return this.thirdParty.map(ThirdParty::springDbName);
    }

  }

}