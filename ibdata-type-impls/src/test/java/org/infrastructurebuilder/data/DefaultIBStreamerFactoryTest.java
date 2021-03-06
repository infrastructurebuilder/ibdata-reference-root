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

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.infrastructurebuilder.IBConstants;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultIBStreamerFactoryTest {
  public final static Logger log = LoggerFactory.getLogger(DefaultIBStreamerFactoryTest.class);
  public final static TestingPathSupplier wps = new TestingPathSupplier();

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  private List<IBDataSpecificStreamFactory<?>> typedSuppliers;
  private IBDataSpecificStreamFactory<String> stringSupplier;
  private DefaultIBStreamerFactory sf;
  private IBDataStream ds;
  private IBDataStreamIdentifier identifier;
  private Path ins;
  private Checksum checksum;

  @Before
  public void setUp() throws Exception {
    ins = wps.getTestClasses().resolve("lines.txt");
    checksum = new Checksum(ins);
    identifier = new DefaultIBDataStreamIdentifier(UUID.randomUUID(), empty(), empty(), empty(), checksum, new Date(),
        IBMetadataUtils.emptyDocumentSupplier.get(), IBConstants.TEXT_PLAIN, empty(),empty(), empty());
    stringSupplier = new DefaultStringStreamSupplier(() -> log);
    typedSuppliers = Arrays.asList(stringSupplier, new DefaultStringStreamSupplier(() -> log), new DefaultStringStreamSupplier(() -> log) {
      @Override
      public int getWeight() {
        return 5;
      }
    });
    sf = new DefaultIBStreamerFactory(typedSuppliers);
    ds = new DefaultIBDataStream(identifier, ins);
  }

  @After
  public void tearDown() throws Exception {
  }

  @SuppressWarnings("unused")
  @Test
  public void testFrom() {
    Stream<String> q = (Stream<String>) sf.from(ds).get();
    List<String> l = q.collect(toList());
    assertEquals(6, l.size());
  }

}
