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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySortedMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.infrastructurebuilder.IBConstants.APPLICATION_OCTET_STREAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.UUID;

import org.infrastructurebuilder.data.ingest.IBDataSchemaIngestionConfig;
import org.infrastructurebuilder.data.model.DataSet;
import org.infrastructurebuilder.data.util.files.DefaultTypeToExtensionMapper;
import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.DefaultConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.infrastructurebuilder.util.files.DefaultIBResource;
import org.infrastructurebuilder.util.files.IBResource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractIBDataSetFinalizerSupplierTest {
  public final static Logger log = LoggerFactory.getLogger(AbstractIBDataSetFinalizerSupplierTest.class);
  public final static TestingPathSupplier wps = new TestingPathSupplier();

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    wps.finalize();
  }

  private AbstractIBDataSetFinalizerSupplier<Dummy, Object> c;
  private DefaultTypeToExtensionMapper t2e;
  private DefaultConfigMapSupplier cms;
  private IBDataSet dsi1;
  private DataSet d;
  private Path dummyPath;

  @Before
  public void setUp() throws Exception {
    dummyPath = wps.get();
    Files.copy(wps.getTestClasses().resolve("rick.jpg"), dummyPath.resolve("rick.jpg"));
    t2e = new DefaultTypeToExtensionMapper();
    cms = new DefaultConfigMapSupplier();
    cms.addValue("path", dummyPath);

    d = new DataSet();
    d.setUuid(UUID.randomUUID().toString());
    d.setCreationDate(new Date());
    d.setArtifactId("A");
    d.setDescription("desc");
    d.setName("name");
    d.setGroupId("G");
    d.setMetadata(new Metadata());
    d.setVersion("1.0");
    dsi1 = new DefaultIBDataSet(d);
    c = getSupplier(log, wps, cms, t2e);

  }

  private AbstractIBDataSetFinalizerSupplier<Dummy, Object> getSupplier(Logger log, TestingPathSupplier wps,
      DefaultConfigMapSupplier cms, DefaultTypeToExtensionMapper t2e) {
    return new AbstractIBDataSetFinalizerSupplier<Dummy, Object>(log, wps, cms, t2e) {

      @Override
      public AbstractIBDataSetFinalizerSupplier<Dummy, Object> getConfiguredSupplier(ConfigMapSupplier cms) {
        return this;
      }

      @Override
      protected IBDataSetFinalizer<Dummy> getInstance(PathSupplier workingPath, Object nothing) {
        return new DummyFinalizer(getConfig().get(), getConfig().get().get("path"));
      }

    };
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testGetTypeToExtensionMapper() {
    assertEquals(t2e, c.getTypeToExtensionMapper());
  }

  @Test(expected = IBDataException.class)
  public void testGetWithExistingPathNotADir() throws IOException {
    cms = new DefaultConfigMapSupplier();
    dummyPath = dummyPath.resolve("X");
    cms.addValue("path", dummyPath);
    IBUtils.writeString(dummyPath, "ABC"); // Creates the file
    c = getSupplier(log, wps, cms, t2e);
    IBDataSetFinalizer<Dummy> v = c.configure(cms).get();

  }

  @Test
  public void testGet() throws IOException {
    IBDataSetFinalizer<Dummy> v = c.configure(cms).get();
    assertNotNull(v.getWorkingPath());
    assertTrue(Files.isDirectory(v.getWorkingPath()));
    IBResource g = v.finalize(dsi1, new Dummy(), emptyList(), empty());
    assertEquals(dummyPath, g.getPath().getParent());
    assertEquals(new Checksum().toString(), g.getChecksum().toString());
  }

  public final static class Dummy implements DataSetEnabled {

    @Override
    public DataSet asDataSet() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public SortedMap<String, IBDataSchemaIngestionConfig> asSchemaIngestion() {
      return emptySortedMap();
    }

  }

  public final static class DummyFinalizer extends AbstractIBDataSetFinalizer<Dummy, Object> {

    public DummyFinalizer(ConfigMap map, Path p) {
      super(map, p);
    }

    @Override
    public IBResource finalize(IBDataSet dsi1, Dummy target, List<IBDataStreamSupplier> suppliers,
        List<IBSchemaDAOSupplier> schemaSuppliers, Optional<String> basedir) throws IOException {
      getConfig();
      return new DefaultIBResource(getWorkingPath().resolve("rick.jpg"), new Checksum(), of(APPLICATION_OCTET_STREAM));
    }

  };

}
