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
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jooq.SQLDialect;

public interface IBDatabaseDialectMapper {

  static final Map<String, SQLDialect> dialects = unmodifiableMap(
      asList(SQLDialect.values()).stream().collect(toMap(SQLDialect::name, identity())));
  static final Map<String, LBDDatabase> liquibaseDatabaseClass = unmodifiableMap(
      asList(LBDDatabase.values()).stream().collect(toMap(LBDDatabase::name, identity())));

  public static Set<SQLDialect> allDialects() {
    return Collections.unmodifiableSet(asList(SQLDialect.values()).stream().collect(toSet()));
  }

  public static Set<LBDDatabase> allLBDialects() {
    return Collections.unmodifiableSet(asList(LBDDatabase.values()).stream().collect(toSet()));
  }

  public static Optional<SQLDialect> bySQLDialectName(String name) {
    return ofNullable(dialects.get(name));
  }

  public static Optional<LBDDatabase> byLiquibaseDatabaseName(SQLDialect dialect) {
    return ofNullable(liquibaseDatabaseClass.get(dialect.name()));
  }

  /**
   * Return an IBDatabaseDialect from a Jooq SQLDialect name()
   * @param jooqDialect name() of a Jooq SQLDialect
   * @return IBDatabaseDialect instance if available
   */
  Optional<IBDatabaseDialect> from(String jooqDialect);
  public static final  String H_2 = "H2";
  public static final String LIQUIBASE_DATABASE_CORE_MY_SQL_DATABASE = "liquibase.database.core.MySQLDatabase";
  public static final String LIQUIBASE_DATABASE_CORE_SQ_LITE_DATABASE = "liquibase.database.core.SQLiteDatabase";
  public static final String LIQUIBASE_DATABASE_CORE_HSQL_DATABASE = "liquibase.database.core.HsqlDatabase";
  public static final String LIQUIBASE_DATABASE_CORE_H2_DATABASE = "liquibase.database.core.H2Database";
  public static final String LIQUIBASE_DATABASE_CORE_MSSQL_DATABASE = "liquibase.database.core.MSSQLDatabase";
  public static final String LIQUIBASE_DATABASE_CORE_MARIA_DB_DATABASE = "liquibase.database.core.MariaDBDatabase";

  public enum LBDDatabase {
    H2(H_2, LIQUIBASE_DATABASE_CORE_H2_DATABASE) // H2
    , HSQLDB("HSQLDB", LIQUIBASE_DATABASE_CORE_HSQL_DATABASE) //SQLite
    , SQLLITE("SQLITE", LIQUIBASE_DATABASE_CORE_SQ_LITE_DATABASE) //SQLite
    , MYSQL("MYSQL", LIQUIBASE_DATABASE_CORE_MY_SQL_DATABASE) // MySql
    , MARIADB("MARIADB", LIQUIBASE_DATABASE_CORE_MARIA_DB_DATABASE) //
    , SQLSERVER("SQLSERVER", LIQUIBASE_DATABASE_CORE_MSSQL_DATABASE) // SQL Server
    , SQLSERVER2008("SQLSERVER2008", LIQUIBASE_DATABASE_CORE_MSSQL_DATABASE) // SQL Server
    , SQLSERVER2014("SQLSERVER2014", LIQUIBASE_DATABASE_CORE_MSSQL_DATABASE) // SQL Server
    , SQLSERVER2016("SQLSERVER2016", LIQUIBASE_DATABASE_CORE_MSSQL_DATABASE) // SQL Server
    , SQLSERVER2017("SQLSERVER2017", LIQUIBASE_DATABASE_CORE_MSSQL_DATABASE) // SQL Server
    //
    ;
    private final Optional<SQLDialect> sqlDialect;
    private final String databaseClass;

    private LBDDatabase(final String sqlDialect, final String databaseClass) {
      this.sqlDialect = bySQLDialectName(Objects.requireNonNull(sqlDialect));
      this.databaseClass = Objects.requireNonNull(databaseClass);
    }

    public Optional<SQLDialect> sqlDialect() {
      return this.sqlDialect;
    }

    public String getDatabaseClass() {
      return this.databaseClass;
    }

    @Override
    public String toString() {
      return ("LBDDatabase [sqlDialect = " + this.sqlDialect.toString() + ", databaseClass = " + this.databaseClass);
    }
  }

}
