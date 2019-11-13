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

import static org.infrastructurebuilder.data.IBDatabaseDialectMapper.H_2;
import static org.infrastructurebuilder.data.IBDatabaseDialectMapper.LIQUIBASE_DATABASE_CORE_H2_DATABASE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.jooq.SQLDialect;
import org.junit.Before;
import org.junit.Test;

public class DefaultIBDatabaseDialectMapperTest {


  private DefaultIBDatabaseDialectMapper d;

  @Before
  public void setUp() throws Exception {
    d = new DefaultIBDatabaseDialectMapper();
  }

  @Test
  public void testFrom() {
    Optional<IBDatabaseDialect> q = d.from(SQLDialect.H2.name());
    assertNotNull(q);
    assertTrue(q.isPresent());
    IBDatabaseDialect v = q.get();
    assertEquals(H_2, v.jooqDialectEnum());
    assertEquals(LIQUIBASE_DATABASE_CORE_H2_DATABASE, v.liquibaseDatabaseClass().get());
    assertEquals(H_2, v.springDbName().get());
    assertEquals("org.hibernate.dialect.H2Dialect", v.hibernateDialectClass().get());
  }

}
