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

import static org.infrastructurebuilder.data.IBDataAvroUtils.avroSchemaFromString;
import static org.infrastructurebuilder.data.IBDataAvroUtils.managedValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.Record;
import org.infrastructurebuilder.data.transform.BA;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class IBDataAvroUtilsTest {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  private Formatters formatters;
  private Schema schema;
  private TestingPathSupplier wps = new TestingPathSupplier();
  private Record r;
  private Map<String, Field> fields;
  private Schema schema2;
  private Map<String, Field> fields2;

  @Before
  public void setUp() throws Exception {
    formatters = new Formatters() {

    };
    Path p = wps.getTestClasses().resolve("ba.avsc");
    schema = avroSchemaFromString.apply(p.toAbsolutePath().toString());
    schema2 = BA.SCHEMA$;
    r = new GenericData.Record(schema);
    fields = schema.getFields().stream().collect(Collectors.toMap(Field::name, Function.identity()));
    fields2 = schema2.getFields().stream().collect(Collectors.toMap(Field::name, Function.identity()));
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testManagedValue() {

    assertNull(managedValue(fields2.get("alive").schema(), null, null, formatters));
    assertTrue((Boolean) managedValue(fields2.get("alive").schema(), null, "true", formatters));
    assertEquals(new Double(7.3), (Double) managedValue(fields2.get("dub").schema(), null, "7.3", formatters));
    assertEquals(new Long(7L), (Long) managedValue(fields2.get("l").schema(), null, "7", formatters));
    assertEquals(new Float(7.3), (Float) managedValue(fields2.get("f").schema(), null, "7.3", formatters));
    assertNull(managedValue(fields2.get("nullType").schema(), null, "7.3", formatters));
    assertEquals(5, ((byte[]) managedValue(fields2.get("bytesType").schema(), null, "12345", formatters)).length);
    assertEquals(new Integer(63120000), (Integer) managedValue(fields2.get("time1").schema(), null, "17:32", formatters));
    assertEquals(new Long(1196676930000L), (Long) managedValue(fields2.get("time2").schema(), null, "2007-12-03T10:15:30.00Z", formatters));
  }

  @Test
  public void testFromUnion() {
  }

  @Test
  public void testFromTypeLogicalType() {
    //    Optional<String> lType;
    //    fromTypeLogicalType(schema, lType, in, formatters);
  }

}
