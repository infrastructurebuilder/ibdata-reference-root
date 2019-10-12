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
package org.infrastructurebuilder.data.transform;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Parser;
import org.apache.avro.generic.GenericRecord;
import org.infrastructurebuilder.data.transform.line.AbstractMapSSToGenericRecordIBDataLineTransformer;
import org.infrastructurebuilder.data.transform.line.DefaultMapSSToGenericRecordIBDataLineTransformer;
import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.config.WorkingPathSupplier;
import org.junit.Before;
import org.junit.Test;

public class AbstractMapStringStringToGenericRecordIBDataLineTransformerTest {

  protected static final String DATE_PATTERN = "yyyy-MM-dd";
  protected static final String TIME_PATTERN = "HH:mm";
  protected static final String TS_PATTERN = DateTimeFormatter.ISO_INSTANT.toString();

  private AbstractMapSSToGenericRecordIBDataLineTransformer test;
  private Schema s;
  private Map<String, String> testData = new HashMap<>();
  private WorkingPathSupplier wps = new WorkingPathSupplier();
  private Path workingPath;

  @Before
  public void setUp() throws Throwable {
    testData.put("column1", "c1");
    testData.put("column2", "c2");
    testData.put("pre1996", "false");
    testData.put("effective", "2099-10-04");
    testData.put("deletion", "2299-10-11");
    Object[] a = Arrays.asList("A", "B", 1, 2, 3).toArray(new Object[0]);
    Properties p1 = new Properties();
    try (InputStream in = getClass().getResourceAsStream("/load1.properties")) {
      p1.load(in);
    }

    try (InputStream ins = getClass().getResourceAsStream("/nccirefdatapackager.avsc")) {
      Parser p = new Schema.Parser();
      s = p.parse(ins);
    }

    workingPath = wps.get();

    test = new DefaultMapSSToGenericRecordIBDataLineTransformer(workingPath, IBUtils.propertiesToMapSS.apply(p1));
  }

  @Test
  public void test() {
    GenericRecord r = test.apply(testData);
    assertNotNull(r);
  }

}
