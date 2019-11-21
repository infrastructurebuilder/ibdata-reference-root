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
package org.infrastructurebuilder.data.ingest;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

import org.infrastructurebuilder.IBConstants;
import org.infrastructurebuilder.data.IBDataConstants;
import org.infrastructurebuilder.data.util.files.DefaultTypeToExtensionMapper;
import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultWGetterSupplierTest {

  private final static Logger log = LoggerFactory.getLogger(DefaultWGetterSupplierTest.class);
  private static TestingPathSupplier wps;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    wps = new TestingPathSupplier();
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    wps.finalize();
  }

  private DefaultWGetterSupplier ws;

  @Before
  public void setUp() throws Exception {
    this.ws = new DefaultWGetterSupplier(() -> log, new DefaultTypeToExtensionMapper(), wps);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testGet() throws IOException {
    WGetter w = this.ws.get();
    Path outputPath = wps.get();

    String sourceString = "http://www.example.com/index.html"; //wps.getTestClasses().resolve("rick.jpg").toUri().toURL().toExternalForm();
    Optional<IBChecksumPathType> q = w.collectCacheAndCopyToChecksumNamedFile(Optional.empty(), outputPath,
        sourceString, Optional.empty(), Optional.empty(), false, 5, 1000, false);
    assertTrue(q.isPresent());
    assertEquals(IBConstants.APPLICATION_OCTET_STREAM, q.get().getType());
    String v = IBUtils.readToString(q.get().get());
    assertTrue(v.contains("/www.iana.org/"));
  }

}
