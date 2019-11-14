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
import static java.util.Optional.ofNullable;

import java.util.Optional;

import org.jooq.SQLDialect;
import org.jooq.SQLDialect.ThirdParty;

public class DefaultIBDatabaseDialectMapper implements IBDatabaseDialectMapper {

  public final Optional<IBDatabaseDialect> from(String jooqDialect) {
    return IBDatabaseDialectMapper.bySQLDialectName(jooqDialect).map(DefaultIBDatabaseDialect::new);
  }

  private final class DefaultIBDatabaseDialect implements IBDatabaseDialect {

    private final SQLDialect jd;
    private final Optional<LBDDatabase> liquibase;
    private final Optional<ThirdParty> thirdParty;

    public DefaultIBDatabaseDialect(SQLDialect d) {
      this.jd = requireNonNull(d);
      this.thirdParty = ofNullable(jd.thirdParty());
      this.liquibase = IBDatabaseDialectMapper.byLiquibaseDatabaseName(jd);
    }

    @Override
    public String jooqDialectEnum() {
      return this.jd.getName();
    }

    @Override
    public Optional<String> hibernateDialectClass() {
      return this.thirdParty.map(ThirdParty::hibernateDialect);
    }

    @Override
    public Optional<String> liquibaseDatabaseClass() {
      return this.liquibase.map(LBDDatabase::getDatabaseClass);
    }

    @Override
    public Optional<String> springDbName() {
      return this.thirdParty.map(ThirdParty::springDbName);
    }

  }

}
