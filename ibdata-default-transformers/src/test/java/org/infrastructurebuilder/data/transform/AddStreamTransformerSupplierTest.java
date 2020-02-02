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

import static org.infrastructurebuilder.data.AbstractModelTest.ARTIFACT;
import static org.infrastructurebuilder.data.AbstractModelTest.DESC;
import static org.infrastructurebuilder.data.AbstractModelTest.GROUP;
import static org.infrastructurebuilder.data.AbstractModelTest.NAME;
import static org.infrastructurebuilder.data.AbstractModelTest.VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.infrastructurebuilder.IBException;
import org.infrastructurebuilder.data.DefaultIBDataSet;
import org.infrastructurebuilder.data.IBDataSet;
import org.infrastructurebuilder.data.IBDataStream;
import org.infrastructurebuilder.data.IBDataStreamRecordFinalizer;
import org.infrastructurebuilder.data.IBDataTransformationResult;
import org.infrastructurebuilder.data.IBDataTransformer;
import org.infrastructurebuilder.data.Metadata;
import org.infrastructurebuilder.data.model.DataSet;
import org.infrastructurebuilder.data.transform.line.StringIBDataStreamRecordFinalizerSupplier;
import org.infrastructurebuilder.util.FakeCredentialsFactory;
import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.artifacts.IBArtifactVersionMapper;
import org.infrastructurebuilder.util.artifacts.impl.DefaultGAV;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.DefaultConfigMapSupplier;
import org.infrastructurebuilder.util.config.FakeIBVersionsSupplier;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;
import org.infrastructurebuilder.util.config.IBRuntimeUtilsTesting;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddStreamTransformerSupplierTest {
  public final static Logger log = LoggerFactory.getLogger(AddStreamTransformerSupplierTest.class);
  public final static TestingPathSupplier wps = new TestingPathSupplier();
  private final static IBRuntimeUtils ibr = new IBRuntimeUtilsTesting(wps, log,
      new DefaultGAV(new FakeIBVersionsSupplier()), new FakeCredentialsFactory(), new IBArtifactVersionMapper() {
      });

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    wps.finalize();
  }

  private ConfigMapSupplier cms;
  private AddStreamTransformerSupplier p;
  private IBDataStreamRecordFinalizer<?> finalizer;
  private StringIBDataStreamRecordFinalizerSupplier finalizerSupplier;
  private List<IBDataStream> suppliedStreams = Collections.emptyList();
  private IBDataSet ds;
  private DataSet finalData;
  private Date now;
  private DefaultGAV gav;
  private Transformer x1;
  private IBTransformation x;
  private Path finalWP;

  @Before
  public void setUp() throws Exception {
    x1 = new Transformer();
    x1.setId("id");
    x1.setHint(PassThruTransformerSupplier.NAME);
    x1.setFailOnAnyError(true);
    x1.setSources(Collections.emptyList());
    x1.setTargetStreamMetadata(new Metadata());
    x = new FakeIBTransformation("id", NAME, DESC, new XmlPlexusConfiguration("metadata"), GROUP, ARTIFACT, VERSION);
    cms = new DefaultConfigMapSupplier();
    p = new AddStreamTransformerSupplier(ibr);
    finalWP = wps.get();
//    .resolve(UUID.randomUUID().toString());
//    Files.createDirectories(finalWP.getParent());
    IBRuntimeUtils ibr = new IBRuntimeUtilsTesting(() -> finalWP, log, new DefaultGAV(new FakeIBVersionsSupplier()),
        new FakeCredentialsFactory(), new IBArtifactVersionMapper() {
        });

    finalizerSupplier = new StringIBDataStreamRecordFinalizerSupplier(ibr);
    finalizer = finalizerSupplier.configure(cms).get();
    finalData = new DataSet();
    finalData.setUuid(UUID.randomUUID().toString());
    now = new Date(1570968733117L);
    finalData.setCreationDate(now);
    finalData.setGroupId(GROUP);
    finalData.setArtifactId(ARTIFACT);
    finalData.setVersion(VERSION);
    finalData.setMetadata(new Metadata());
    gav = new DefaultGAV(GROUP, ARTIFACT, VERSION);
    finalData.setModelVersion("1.0");
    finalData.setDescription(DESC);
    finalData.setName(NAME);

    ds = new DefaultIBDataSet(finalData);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testSkipRows() {
  }

  @Test
  public void testAccepts() {
  }

  @Test(expected = FileAlreadyExistsException.class)
  public void testGetWriter() throws IOException {
    assertTrue(Files.exists(finalizer.getWorkingPath()));

    ByteArrayInputStream source = new ByteArrayInputStream("ABC".getBytes());
    try (OutputStream outs = finalizer.getWriterTarget()) {
      IBUtils.copy(source, outs);
    }
    assertTrue(Files.exists(finalizer.getWorkingPath()));
  }

  @Test
  public void testConfigureConfigMapSupplier() throws IOException {
    assertEquals(0, finalizer.getNumberOfRowsToSkip());
    assertEquals(Arrays.asList(String.class), finalizer.accepts().get());
    Path p44 = wps.get().resolve("ABCD.txt");
    IBUtils.writeString(p44, "String");
    cms.addValue(AddStreamTransformerSupplier.ADDED_PATH, p44.toAbsolutePath().toString());
    p = (AddStreamTransformerSupplier) p.withFinalizer(finalizer).configure(cms);
    IBDataTransformer t = p.get();
    assertEquals(AddStreamTransformerSupplier.ADD_STREAM, t.getHint());
    assertFalse(t.respondsTo(null));
    IBDataTransformationResult q = t.transform(x1, ds, suppliedStreams, true);
    assertTrue(q.get().isPresent());
  }

  @Test(expected = IBException.class)
  public void testUncnfiguredConfigureConfigMapSupplier() throws IOException {
    p = (AddStreamTransformerSupplier) p.withFinalizer(finalizer).configure(cms);

    IBDataTransformer t = p.get();
    assertEquals(AddStreamTransformerSupplier.ADD_STREAM, t.getHint());
    assertFalse(t.respondsTo(null));
    IBDataTransformationResult q = t.transform(x1, ds, suppliedStreams, true);
    assertTrue(q.get().isPresent());
  }

  @Test
  public void testFinalizerGet() {
    InputStream k = finalizer.get();
    assertNotNull(k);
  }
}
