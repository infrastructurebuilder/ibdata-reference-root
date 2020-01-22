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

import static org.infrastructurebuilder.IBConstants.DEFAULT;
import static org.infrastructurebuilder.IBConstants.DESCRIPTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.infrastructurebuilder.IBConstants;
import org.infrastructurebuilder.data.DefaultIBDataStream;
import org.infrastructurebuilder.data.DefaultIBDataStreamSupplier;
import org.infrastructurebuilder.data.IBDataStream;
import org.infrastructurebuilder.data.IBDataStreamIdentifier;
import org.infrastructurebuilder.data.IBDataStreamSupplier;
import org.infrastructurebuilder.data.IBSchema;
import org.infrastructurebuilder.data.IBSchemaDAO;
import org.infrastructurebuilder.data.IBSchemaDAOSupplier;
import org.infrastructurebuilder.data.IBSchemaSource;
import org.infrastructurebuilder.data.Metadata;
import org.infrastructurebuilder.data.model.DataStream;
import org.infrastructurebuilder.data.model.PersistedIBSchema;
import org.infrastructurebuilder.data.model.io.xpp3.PersistedIBSchemaXpp3Writer;
import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.infrastructurebuilder.util.files.DefaultIBResource;
import org.infrastructurebuilder.util.files.IBResource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DefaultIBSchemaDAOSupplierTest {
  private final static TestingPathSupplier wps = new TestingPathSupplier();

  private static final String ID = FakeIBSchemaSource.ID;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  private IBSchemaDAO dao;
  private String tempId;
  private IBSchemaSource<Object> src;
  private Map<String, IBDataStreamSupplier> map;
  private IBSchemaDAOSupplierBuilder b;
  private DefaultIBDataSchemaIngestionConfig d1;

  private PersistedIBSchema schema;

  private Path ins;

  private Path path;

  @Before
  public void setUp() throws Exception {
    d1 = new DefaultIBDataSchemaIngestionConfig();
    d1.setTemporaryId(ID);
    d1.setDescription(DESCRIPTION);
    schema = new PersistedIBSchema().clone();
    map = new HashMap<>();
    path = wps.get().resolve(UUID.randomUUID().toString());
    try (Writer w = Files.newBufferedWriter(path)) {
      PersistedIBSchemaXpp3Writer writer = new PersistedIBSchemaXpp3Writer();
      writer.write(w, schema);
    }
    Checksum c = new Checksum(path);

    DataStream i = new DataStream();
    i.setCreationDate(new Date());
    i.setMetadata(new Metadata());
    i.setSha512(c.toString());
    IBDataStream ds = new DefaultIBDataStream(i, path);
    IBDataStreamSupplier v = new DefaultIBDataStreamSupplier(ds);
    map.put(DEFAULT, v);
    src = new FakeIBSchemaSource<Object>() {
      public java.util.Map<String, org.infrastructurebuilder.util.files.IBResource> get() {
        HashMap<String, IBResource> x = new HashMap<>();
        x.put(DEFAULT, new DefaultIBResource(path, c, Optional.of(IBConstants.APPLICATION_OCTET_STREAM)));
        return x;
      };
    };
    b = DefaultIBSchemaDAOSupplierBuilder.newInstance().withSource(src).withIngestionConfig(d1);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testGet() {
    IBSchemaDAOSupplier s = b.build();
    assertNotNull(s.get());
  }

  @Test
  public void testId() {
    IBSchemaDAOSupplier s = b.build();
    assertEquals(ID, s.getTemporaryId());
  }

  @Test
  public void testDAOStuff() {
    IBSchemaDAO d = b.build().get();
    assertEquals(1, d.get().size());
    assertFalse(d.getOriginalAssetKeyName().isPresent());
    assertEquals(DEFAULT, d.getPrimaryAssetKeyName());
    IBSchema ds = d.getSchema();
    assertEquals(schema, ds);
  }

}
