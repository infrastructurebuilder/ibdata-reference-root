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

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.infrastructurebuilder.data.IBDataConstants.H_2;
import static org.infrastructurebuilder.data.IBDataConstants.LIQUIBASE_DATABASE_CORE_H2_DATABASE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.infrastructurebuilder.data.h2.H2DatabaseDriverSupplier;
import org.infrastructurebuilder.util.BasicCredentials;
import org.infrastructurebuilder.util.CredentialsFactory;
import org.infrastructurebuilder.util.DefaultBasicCredentials;
import org.infrastructurebuilder.util.DefaultURLAndCreds;
import org.infrastructurebuilder.util.URLAndCreds;
import org.infrastructurebuilder.util.artifacts.GAV;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.infrastructurebuilder.util.files.IBResource;
import org.jooq.SQLDialect;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultIBDatabaseDialectMapperTest {
  private static final String JDBC_H2 = "jdbc:h2:";
  private static final URLAndCreds jdbcH2 = new DefaultURLAndCreds(JDBC_H2);

  private static final Logger log = LoggerFactory.getLogger(DefaultIBDatabaseDialectMapperTest.class);
  public final static TestingPathSupplier wps = new TestingPathSupplier();

  private static final String ORG_HIBERNATE_DIALECT_H2_DIALECT = "org.hibernate.dialect.H2Dialect";
  private DefaultIBDatabaseDialectMapper d;

  private CredentialsFactory cf;

  @Before
  public void setUp() throws Exception {
    cf = new CredentialsFactory() {

      @Override
      public Optional<BasicCredentials> getCredentialsFor(String query) {
        return of(new DefaultBasicCredentials("SA", empty()));
      }
    };
    Map<String, IBDataDatabaseDriverSupplier> z = new HashMap<>();
    IBDataDatabaseDriverSupplier kk = new H2DatabaseDriverSupplier(wps, () -> log, cf);
    IBDataDatabaseDriverSupplier i = new IBDataDatabaseDriverSupplier() {

      @Override
      public Logger getLog() {
        return log;
      }

      @Override
      public List<GAV> getRequiredArtifacts() {
        return Collections.emptyList();
      }

      @Override
      public String getHint() {
        return "NADA";
      }

      @Override
      public Optional<IBDatabaseDialect> getDialect(URLAndCreds jdbcUrl) {
        return of(new IBDatabaseDialect() {

          @Override
          public String getJooqName() {
            return "NADA";
          }

          @Override
          public Optional<String> springDbName() {
            return of(getJooqName());
          }

          @Override
          public String liquibaseDatabaseClass() {
            return LIQUIBASE_DATABASE_CORE_H2_DATABASE;
          }

          @Override
          public Optional<String> hibernateDialectClass() {
            return of(ORG_HIBERNATE_DIALECT_H2_DIALECT);
          }

          @Override
          public Map<String, Object> getDbUnitConfigurationUpdates() {
            return emptyMap();
          }
        });
      }

      @Override
      public Optional<String> getDatabaseDriverClassName(URLAndCreds jdbcUrl) {
        return of("ABCDEFG");
      }

      @Override
      public boolean respondsTo(URLAndCreds jdbcURL) {
        return false;
      }

      @Override
      public Optional<Supplier<DataSource>> getDataSourceSupplier(URLAndCreds in) {
        return empty();
      }

      @Override
      public CredentialsFactory getCredentialsFactory() {
        return cf;
      }

      @Override
      public Optional<Map<String, IBResource>> schemaFrom(URLAndCreds in, String query, String nameSpace, String name,
          Optional<String> desc) {
        // TODO Auto-generated method stub
        return empty();
      }
    };
    i.getLog().debug("Test Setup");
    z.put(SQLDialect.H2.name(), kk);
    z.put("NADA", i);
    d = new DefaultIBDatabaseDialectMapper(z);
  }

  @Test
  public void testFrom() {
    Optional<IBDatabaseDialect> q = d.getSupplierForURL(jdbcH2).get().getDialect(jdbcH2);
    assertNotNull(q);
    assertTrue(q.isPresent());
    IBDatabaseDialect v = q.get();
    assertEquals(H_2, v.getJooqName());
    assertEquals(LIQUIBASE_DATABASE_CORE_H2_DATABASE, v.liquibaseDatabaseClass());
    assertEquals(H_2, v.springDbName().get());
    assertEquals(ORG_HIBERNATE_DIALECT_H2_DIALECT, v.hibernateDialectClass().get());
  }

}
