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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.generic.GenericRecord;
import org.infrastructurebuilder.data.IBDataAvroUtils;
import org.infrastructurebuilder.data.IBDataDataStreamRecordFinalizerSupplier;
import org.infrastructurebuilder.data.IBDataStreamRecordFinalizer;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.DefaultConfigMapSupplier;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class GenericAvroIBDataRecordFinalizerSupplierTest {

  private final static TestingPathSupplier wps = new TestingPathSupplier();

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    wps.finalize();
  }

  private GenericAvroIBDataRecordFinalizerSupplier g;
  private ConfigMapSupplier cms;
  private GenericRecord record;
  private Schema schema;

  @Before
  public void setUp() throws Exception {
    cms = new DefaultConfigMapSupplier();
    String schemaString = wps.getTestClasses().resolve("ba.avsc").toAbsolutePath().toString();
    cms.addValue(DefaultMapToGenericRecordIBDataLineTransformerSupplier.SCHEMA_PARAM,
        schemaString );
    cms.addValue(IBDataStreamRecordFinalizer.NUMBER_OF_ROWS_TO_SKIP_PARAM, "1");
    g = new GenericAvroIBDataRecordFinalizerSupplier(wps);
    schema = IBDataAvroUtils.getSchema.apply(schemaString);
    record = new Record(schema);

  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testConfigure() {
    IBDataDataStreamRecordFinalizerSupplier<GenericRecord> h = g.config(cms);
    assertFalse(g == h);
  }

  @Test
  public void testGet() {
    IBDataStreamRecordFinalizer<GenericRecord> q = g.config(cms).get();
    assertEquals(1, q.getNumberOfRowsToSkip());
    q.writeRecord(record);

  }

}
