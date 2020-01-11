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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.generic.GenericRecord;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.infrastructurebuilder.data.DefaultAvroGenericRecordStreamSupplier;
import org.infrastructurebuilder.data.DefaultAvroGenericRecordStreamSupplierTest;
import org.infrastructurebuilder.data.DefaultGenericDataSupplier;
import org.infrastructurebuilder.data.DefaultIBDataAvroUtilsSupplier;
import org.infrastructurebuilder.data.DefaultIBDataStream;
import org.infrastructurebuilder.data.GenericDataSupplier;
import org.infrastructurebuilder.data.IBDataAvroUtils;
import org.infrastructurebuilder.data.IBDataAvroUtilsSupplier;
import org.infrastructurebuilder.data.IBDataDataStreamRecordFinalizerSupplier;
import org.infrastructurebuilder.data.IBDataStreamRecordFinalizer;
import org.infrastructurebuilder.data.IBDataStructuredDataFieldMetadata;
import org.infrastructurebuilder.data.Metadata;
import org.infrastructurebuilder.data.model.DataStream;
import org.infrastructurebuilder.data.transform.line.GenericAvroIBDataRecordFinalizerSupplier.GenericAvroIBDataStreamRecordFinalizer;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.DefaultConfigMapSupplier;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericAvroIBDataRecordFinalizerSupplierTest {

  public final static Logger log = LoggerFactory.getLogger(GenericAvroIBDataRecordFinalizerSupplierTest.class);
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
  private IBDataAvroUtilsSupplier aus;
  private GenericDataSupplier gds;
  private String schemaString;

  @Before
  public void setUp() throws Exception {
    cms = new DefaultConfigMapSupplier();
    schemaString = wps.getTestClasses().resolve("ba.avsc").toAbsolutePath().toString();
    cms.addValue(DefaultMapToGenericRecordIBDataLineTransformerSupplier.SCHEMA_PARAM, schemaString);
    cms.addValue(IBDataStreamRecordFinalizer.NUMBER_OF_ROWS_TO_SKIP_PARAM, "1");
    gds = new DefaultGenericDataSupplier(() -> log);
    aus = new DefaultIBDataAvroUtilsSupplier(() -> log, gds);
    g = new GenericAvroIBDataRecordFinalizerSupplier(wps, () -> log, aus);

  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testConfigure() {
    IBDataDataStreamRecordFinalizerSupplier<GenericRecord> h = g.configure(cms);
    assertFalse(g == h);
  }

  @Test
  public void testGet() {
    IBDataDataStreamRecordFinalizerSupplier<GenericRecord> f1 = g.configure(cms);
    IBDataStreamRecordFinalizer<GenericRecord> q = f1.get();
    assertEquals(1, q.getNumberOfRowsToSkip());
    aus = (IBDataAvroUtilsSupplier) aus.configure(cms.get());
    schema = aus.get().avroSchemaFromString(schemaString);
    record = new Record(schema);
    q.writeRecord(record);
  }

  @Test
  public void testAccreteDate() throws Exception {
    aus = (IBDataAvroUtilsSupplier) aus.configure(cms.get());
    schema = aus.get().avroSchemaFromString(schemaString);
    record = new Record(schema);
    List<GenericRecord> l = new ArrayList<>();
    DataStream id = new DataStream();
    id.setUuid(UUID.randomUUID().toString());
    id.setCreationDate(new Date());
    id.setSha512(DefaultAvroGenericRecordStreamSupplierTest.CHECKSUM);
    id.setMetadata(new Metadata());
    DefaultIBDataStream identifier = new DefaultIBDataStream(id, wps.getTestClasses().resolve("ba.avro"));
    DefaultAvroGenericRecordStreamSupplier d = new DefaultAvroGenericRecordStreamSupplier();
    Optional<Stream<GenericRecord>> q = d.from(identifier);
    assertTrue(q.isPresent());
    List<GenericRecord> w = q.get().collect(Collectors.toList());
    GenericAvroIBDataStreamRecordFinalizer f = (GenericAvroIBDataStreamRecordFinalizer) g.configure(cms).get();

    w.forEach(record -> {
      f.writeRecord(record);
    });
    f.close();

    Map<Integer, ? extends IBDataStructuredDataFieldMetadata> smd = f.getStructuredMetadata().get().getFieldMap();
    assertEquals(8, smd.size());
    assertFalse(smd.get(5).isNulled().isPresent());
    assertEquals(13, smd.get(4).getMaxIntValue().get().intValue());
    assertEquals(6, smd.get(4).getMinIntValue().get().intValue());
  }
}
