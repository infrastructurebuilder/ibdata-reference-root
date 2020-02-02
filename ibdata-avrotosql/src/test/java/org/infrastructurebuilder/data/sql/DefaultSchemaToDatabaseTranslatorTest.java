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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.infrastructurebuilder.data.DefaultIBDatabaseDialectMapper;
import org.infrastructurebuilder.data.IBDataDatabaseDriverSupplier;
import org.infrastructurebuilder.data.IBDatabaseDialectMapper;
import org.infrastructurebuilder.data.IBSchema;
import org.infrastructurebuilder.data.model.PersistedIBSchema;
import org.infrastructurebuilder.data.schema.IBSchemaTranslator;
import org.infrastructurebuilder.data.sqlite.SQLiteDatabaseDriverSupplier;
import org.infrastructurebuilder.util.CredentialsFactory;
import org.infrastructurebuilder.util.FakeCredentialsFactory;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.URLAndCreds;
import org.infrastructurebuilder.util.artifacts.IBArtifactVersionMapper;
import org.infrastructurebuilder.util.artifacts.impl.DefaultGAV;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.DefaultConfigMapSupplier;
import org.infrastructurebuilder.util.config.FakeIBVersionsSupplier;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;
import org.infrastructurebuilder.util.config.IBRuntimeUtilsTesting;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSchemaToDatabaseTranslatorTest {
  public final static Logger log = LoggerFactory.getLogger(DefaultSchemaToDatabaseTranslatorTest.class);

  private final static IBRuntimeUtilsTesting ibr = new IBRuntimeUtilsTesting(log);

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    ibr.getTestingPathSupplier().finalize();
  }

  private LoggerSupplier l = () -> log;

  private List<IBDataDatabaseDriverSupplier> suppliers;

  private IBDatabaseDialectMapper dMapper;

  private DefaultLiquibaseSupplier ls;

  private DefaultSchemaToLiquibaseTranslatorSupplier s2d;

  private ConfigMap cm;

  private DefaultConfigMapSupplier cms;

  private List<IBSchema> s;

  private PersistedIBSchema schema;

  @Before
  public void setUp() throws Exception {

    schema = new PersistedIBSchema();
    schema.setNameSpace("");
    schema.setName("name");
    s = Arrays.asList(schema
//        new DefaultIBDataAvroUtilsSupplier(wps, () -> log, new DefaultGenericDataSupplier(wps, () -> log))
//        .get().avroSchemaFromString(v.toExternalForm())

    );
    suppliers = Arrays.asList(new SQLiteDatabaseDriverSupplier(ibr));
    dMapper = new DefaultIBDatabaseDialectMapper(suppliers.stream().collect(toMap(k -> k.getHint(), identity())));
    ls = new DefaultLiquibaseSupplier(ibr, suppliers, dMapper);
    cm = new ConfigMap();
    String s = ibr.getTestingPathSupplier().getTestClasses().resolve("test.trace.db").toAbsolutePath().toString();
    String jdbcurl = "jdbc:sqlite:" + s;
    cms = new DefaultConfigMapSupplier(cm);
    cm.put(URLAndCreds.SOURCE_URL, jdbcurl);
    s2d = (DefaultSchemaToLiquibaseTranslatorSupplier) new DefaultSchemaToLiquibaseTranslatorSupplier(ibr, ls)
        .configure(cms);
//    URL v = wps.getTestClasses().resolve("ba.avsc").toAbsolutePath().toUri().toURL();
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testApply() throws IOException {
    IBSchemaTranslator<IBSchema, LiquibaseDAO> m = s2d.get();
    Optional<List<LiquibaseDAO>> pp = m.to(s);
    assertTrue(pp.isPresent());
    List<LiquibaseDAO> ppg = pp.get();
    assertEquals(1, ppg.size());
    assertFalse(Files.size(ppg.get(0).getPath()) == 0);
  }

}
