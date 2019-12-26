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

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.generic.GenericRecord;
import org.infrastructurebuilder.IBException;
import org.infrastructurebuilder.data.transform.BA;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.DefaultConfigMapSupplier;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IBDataAvroUtilsTest {
  public final static Logger log = LoggerFactory.getLogger(IBDataAvroUtilsTest.class);
  private final static TestingPathSupplier wps = new TestingPathSupplier();
  private Schema schema;
  private Record r;
  private Map<String, Field> fields;
  private Schema schema2;
  private Map<String, Field> fields2;
  private IBDataAvroUtilsSupplier aus;
  private GenericDataSupplier gds;
  private IBDataAvroUtils au;
  private DefaultConfigMapSupplier cms;

  @Before
  public void setUp() throws Exception {

    ConfigMap init= new ConfigMap();
    cms = new DefaultConfigMapSupplier(init);
    gds = new DefaultGenericDataSupplier(() -> log);
    aus = new DefaultIBDataAvroUtilsSupplier(() -> log, gds).configure(init);
    au = aus.get();
    Path p = wps.getTestClasses().resolve("ba.avsc");
    schema = au.avroSchemaFromString(p.toAbsolutePath().toString());
    schema2 = BA.SCHEMA$;
    r = new GenericData.Record(schema);
    fields = schema.getFields().stream().collect(Collectors.toMap(Field::name, Function.identity()));
    fields2 = schema2.getFields().stream().collect(Collectors.toMap(Field::name, Function.identity()));
  }

  @AfterClass
  public static void afterClass() {
    wps.finalize();
  }

  @Test
  public void testFromSchemaAndPathAndTranslator() throws IOException {

    Path targetPath = wps.get();
    DataFileWriter<GenericRecord> d = au
        .fromSchemaAndPathAndTranslator(targetPath.resolve(UUID.randomUUID().toString() + ".avro"), schema);
    assertNotNull(d);
    d.close();
    d = au.fromSchemaAndPathAndTranslator(targetPath.resolve(UUID.randomUUID().toString() + ".avro"), schema);
    assertNotNull(d);
    d.close();

  }

  @Test(expected = IBException.class)
  public void testNotObvioyslyBrokenURLZip() {
    au.avroSchemaFromString("zip:file:/nope.jar");
  }

  @Test(expected = IBDataException.class)
  public void testNotObvioyslyBrokenURLHttp() {
    au.avroSchemaFromString("http://www.example.com");
  }

  @Test(expected = IBDataException.class)
  public void testNotObvioyslyBrokenURLHttps() {
    au.avroSchemaFromString("https://www.example.com");
  }

  @Test(expected = IBException.class)
  public void testNotObvioyslyBrokenURLJar() {
    au.avroSchemaFromString("jar:file:/nope.zip");
  }

  @Test(expected = IBDataException.class)
  public void testNotObvioyslyBrokenURLFile() {
    au.avroSchemaFromString("file:/nopw.www.example.com");
  }

  @Test(expected = IBDataException.class)
  public void testNulled() {
    au.avroSchemaFromString(null);
  }

  @Test(expected = IBDataException.class)
  public void testBrokenURL() {
    au.avroSchemaFromString("noep:@3");
  }

  @Test(expected = IBDataException.class)
  public void testFromMapAndWpNulled() {
    au.fromMapAndWP(wps.getTestClasses(), null);
  }
}
