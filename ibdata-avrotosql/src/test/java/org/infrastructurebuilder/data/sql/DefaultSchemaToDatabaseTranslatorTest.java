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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.avro.Schema;
import org.infrastructurebuilder.data.IBDataAvroUtils;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DefaultSchemaToDatabaseTranslatorTest {

  private static TestingPathSupplier wps;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    wps = new TestingPathSupplier();
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    wps.finalize();
  }

  private SchemaToDatabaseTranslator s2d;
  private List<Schema> s;

  @Before
  public void setUp() throws Exception {
    s2d = new DefaultSchemaToDatabaseTranslator(wps);
    URL v = wps.getTestClasses().resolve("ba.avsc").toAbsolutePath().toUri().toURL();
    s = Arrays.asList(IBDataAvroUtils.avroSchemaFromString.apply(v.toExternalForm()));
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testApply() throws IOException {
    Path q = s2d.apply(s);
    assertFalse(Files.size(q) == 0);
  }

}
