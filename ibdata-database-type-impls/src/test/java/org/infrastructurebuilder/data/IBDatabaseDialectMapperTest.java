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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.infrastructurebuilder.data.IBDatabaseDialectMapper.LBDDatabase;
import org.jooq.SQLDialect;
import org.junit.Test;

public class IBDatabaseDialectMapperTest {

  @Test
  public void testAllDialects() {
    assertEquals(11, IBDatabaseDialectMapper.allDialects().size());
  }

  @Test
  public void testAllLBDialects() {
    assertEquals(10, IBDatabaseDialectMapper.allLBDialects().size());
  }

  @Test
  public void testBySQLDialectName() {
    assertNotNull(IBDatabaseDialectMapper.bySQLDialectName(IBDatabaseDialectMapper.H_2));
  }

  @Test
  public void testByLiquibaseDatabaseName() {
    LBDDatabase l = IBDatabaseDialectMapper.byLiquibaseDatabaseName(SQLDialect.H2).get();
    assertNotNull(l);
    assertEquals("LBDDatabase [sqlDialect = Optional[H2], databaseClass = liquibase.database.core.H2Database", l.toString());
    assertEquals(IBDatabaseDialectMapper.LIQUIBASE_DATABASE_CORE_H2_DATABASE, l.getDatabaseClass());
    assertEquals(SQLDialect.H2, l.sqlDialect().get());
  }

}
