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
package org.infrastructurebuilder.data.transform.line;

import static org.junit.Assert.*;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import org.apache.avro.generic.GenericRecord;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.transform.line.DefaultMapSSToGenericRecordIBDataLineTransformerSupplier.DefaultMapSSToGenericRecordIBDataLineTransformer;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.DefaultConfigMapSupplier;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DefaultMapSSToGenericRecordIBDataLineTransformerSupplierTest {

  public static final String NCCIREFDATAPACKAGER = "nccirefdatapackager";
  private final static TestingPathSupplier wps = new TestingPathSupplier();

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    wps.finalize();
  }

  private Path wp;
  private DefaultMapSSToGenericRecordIBDataLineTransformerSupplier s;
  private ConfigMapSupplier cms;
  private ConfigMap cm;
  private Path schemaFile;

  @Before
  public void setUp() throws Exception {
    schemaFile = wps.getTestClasses().resolve(NCCIREFDATAPACKAGER + ".avsc").toAbsolutePath();
    cm = new ConfigMap();
    cm.put(DefaultMapSSToGenericRecordIBDataLineTransformerSupplier.SCHEMA_PARAM, schemaFile.toString());
    wp = wps.get();
    cms = new DefaultConfigMapSupplier().addConfiguration(cm);
    s = new DefaultMapSSToGenericRecordIBDataLineTransformerSupplier(wps);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testGetHint() {
    assertEquals(DefaultMapSSToGenericRecordIBDataLineTransformerSupplier.NAME, s.getHint());
  }

  @Test
  public void testConfigureConfigMapSupplier() {
    AbstractIBDataRecordTransformerSupplier<Map<String, String>, GenericRecord> v = s.configure(cms);
    assertFalse(v == s);

    DefaultMapSSToGenericRecordIBDataLineTransformer q = (DefaultMapSSToGenericRecordIBDataLineTransformer) v.get();

    assertTrue(q.isBlankFieldNullInUnion());
    assertEquals(NCCIREFDATAPACKAGER, q.getSchema().getName());
    assertEquals(Locale.getDefault(), q.getLocale());
    assertNotNull(q.getTimeFormatter());
    assertNotNull(q.getDateFormatter());
    assertNotNull(q.getTimestampFormatter());

  }

  @Test(expected = IBDataException.class)
  public void testConfigureConfigMapSupplierNoSchema() {
    AbstractIBDataRecordTransformerSupplier<Map<String, String>, GenericRecord> v = s
        .configure(new DefaultConfigMapSupplier());
    assertFalse( v == s);
    v.get();
  }

}
