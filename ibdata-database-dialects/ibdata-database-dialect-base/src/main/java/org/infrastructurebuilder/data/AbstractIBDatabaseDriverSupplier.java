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

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.infrastructurebuilder.data.IBDataJooqUtils.ibSchemaFromRecordResults;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.infrastructurebuilder.util.BasicCredentials;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.artifacts.GAV;
import org.infrastructurebuilder.util.artifacts.impl.DefaultGAV;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.SQLDialect.ThirdParty;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.slf4j.Logger;

import liquibase.database.Database;

abstract public class AbstractIBDatabaseDriverSupplier
    implements IBDataDatabaseDriverSupplier {

  private static final String FAILED_TO_QUERY_SCHEMA_FOR = "Failed to query schema for ";
  private final String hint;
  private final String databaseClass;
  private final List<DefaultGAV> gavs;
  private final Database database;
  private final Logger log;

  protected AbstractIBDatabaseDriverSupplier(LoggerSupplier l, String hint, String liquibaseDatabaseClass,
      String... list) {
    this.log = requireNonNull(l, "LoggerSupplier").get();
    this.hint = requireNonNull(hint, "Driver hint");
    this.databaseClass = requireNonNull(liquibaseDatabaseClass, "Liquibase Database Classname");
    Database db;
    try {
      db = (Database) Class.forName(databaseClass).newInstance();
    } catch (Exception e) {
      db = null;
    }
    this.database = db;
    this.gavs = ofNullable(list).map(Arrays::asList).orElse(Collections.emptyList())
        // Map list to list of GAVs
        .stream().map(DefaultGAV::new).collect(toList());
  }

  @Override
  public Logger getLog() {
    return this.log;
  }

  @Override
  public Optional<IBDatabaseDialect> getDialect(String jdbcUrl) {
    return getDatabaseDriverClassName(jdbcUrl).flatMap(driver -> IBDataDatabaseUtils.bySQLDialectName(getJooqName())
        .map(j -> new DefaultIBDatabaseDialect(j, this.databaseClass)));
  }

  protected final Optional<Database> getDatabase() {
    return ofNullable(this.database);
  }

  public List<GAV> getRequiredArtifacts() {
    return unmodifiableList(this.gavs);
  }

  @Override
  public String getHint() {
    return hint;
  }

  @Override
  public Optional<String> getDatabaseDriverClassName(String jdbcUrl) {
    return ofNullable(jdbcUrl).flatMap(url -> getDatabase().map(db -> db.getDefaultDriver(url)));
  }

  @Override
  public boolean respondsTo(String jdbcURL) {
    return getDatabaseDriverClassName(jdbcURL).isPresent();
  }

  @Override
  public Optional<Supplier<DataSource>> getDataSourceSupplier(URLAndCreds u) {
    return getDatabaseDriverClassName(u.getUrl()).map(driverClass -> {
      // cet.withTranslation(() -> Class.forName(driverClass));
      final BasicDataSource d = new BasicDataSource(); // FIXME Ensure that d is closed
      d.setDriverClassName(driverClass);
      d.setUrl(u.getUrl());
      u.getCreds().ifPresent(cr -> {
        getLog().debug("Using creds " + cr);
        d.setUsername(cr.getKeyId());
        cr.getSecret().ifPresent(secret -> d.setPassword(secret));
      });
      return () -> d;
    });

  }

  @Override
  public Optional<IBSchema> schemaFrom(URLAndCreds in, String query, String nameSpace, String name,
      Optional<String> desc) {
    if (respondsTo(requireNonNull(in.getUrl()))) {
      Optional<Supplier<DataSource>> s = getDataSourceSupplier(in);
      return s.flatMap(dss -> {
        DataSource ds = dss.get();
        IBDatabaseDialect d = getDialect(in.getUrl())
            .orElseThrow(() -> new IBDataException("Unknown dialect " + in.getUrl()));
        SQLDialect dialect = SQLDialect.valueOf(d.get());
        IBSchema k;
        try (DSLContext create = DSL.using(ds.getConnection(), dialect)) {
          final Result<Record> firstResult = create.fetch(query);
          k = ibSchemaFromRecordResults(getLog(), nameSpace, name, desc.orElse(null), in.getUrl(), firstResult,
              IbdataDatabaseTypeImplsVersioning.apiVersion());
        } catch (DataAccessException | SQLException e) {
          getLog().error(FAILED_TO_QUERY_SCHEMA_FOR + in.getUrl());
          // Return empty after closing DSLContext
          k = null;
        }
        return ofNullable(k);
      });
    }
    return empty();
  }

  private final class DefaultIBDatabaseDialect implements IBDatabaseDialect {

    private final SQLDialect jooqDialect;
    private final String liquibase;
    private final Optional<ThirdParty> thirdParty;

    public DefaultIBDatabaseDialect(SQLDialect d, String liquibaseDatabaseClass) {
      this.jooqDialect = requireNonNull(d);
      this.thirdParty = ofNullable(jooqDialect.thirdParty());
      this.liquibase = requireNonNull(liquibaseDatabaseClass);
    }

    @Override
    public String get() {
      return this.jooqDialect.getName();
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