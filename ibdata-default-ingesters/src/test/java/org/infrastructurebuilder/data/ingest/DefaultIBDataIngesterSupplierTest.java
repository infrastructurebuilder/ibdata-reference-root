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

import static java.nio.file.Files.newInputStream;
import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static org.infrastructurebuilder.IBConstants.TEXT_PLAIN;
import static org.infrastructurebuilder.util.files.DefaultIBChecksumPathType.copyToDeletedOnExitTempChecksumAndPath;
import static org.infrastructurebuilder.util.files.DefaultIBChecksumPathType.copyToTempChecksumAndPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Supplier;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.infrastructurebuilder.data.DefaultIBDataSet;
import org.infrastructurebuilder.data.DefaultTestingSource;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBDataIngester;
import org.infrastructurebuilder.data.IBDataSet;
import org.infrastructurebuilder.data.IBDataSetFinalizer;
import org.infrastructurebuilder.data.IBDataSource;
import org.infrastructurebuilder.data.IBDataSourceSupplier;
import org.infrastructurebuilder.data.IBDataStream;
import org.infrastructurebuilder.data.IBDataStreamIdentifier;
import org.infrastructurebuilder.data.Metadata;
import org.infrastructurebuilder.data.model.DataSet;
import org.infrastructurebuilder.data.util.files.DefaultTypeToExtensionMapper;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.DefaultConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;

public class DefaultIBDataIngesterSupplierTest {
  public final static Logger log = getLogger(DefaultIBDataIngesterSupplierTest.class);
  public final static TestingPathSupplier wps = new TestingPathSupplier();

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    wps.finalize();
  }

  private DefaultIBDataIngesterSupplier dis;
  private ConfigMap configMap;
  private DefaultConfigMapSupplier cms;
  private IBDataIngester c;
  private SortedMap<String, IBDataSourceSupplier> dss, dssFail, dssPass;
  private DefaultIBDataSetIdentifier dsi;
  private Ingestion i;
  private IBDataSourceSupplierMapper dssm, dssmFail, dssmPass;
  private TypeToExtensionMapper t2e = new DefaultTypeToExtensionMapper();
  private IBDataSourceSupplier k;
  private DefaultIBDataSetIngestionFinalizerSupplier ibdfs;
  private IBDataSet ibdataset;
  private Date now = new Date();
  private Checksum filesDotTxtChecksum;

  @Before
  public void setUp() throws Exception {
    Path f = wps.getTestClasses().resolve("file.txt").toAbsolutePath();
    filesDotTxtChecksum = new Checksum(f);
    PathSupplier cache = wps;
    configMap = new ConfigMap();
    // configMap.put(CACHE_DIRECTORY_CONFIG_ITEM, cache.toString());
    cms = new DefaultConfigMapSupplier(configMap);
    ibdfs = new DefaultIBDataSetIngestionFinalizerSupplier(wps, () -> log, t2e);
    ibdfs = (DefaultIBDataSetIngestionFinalizerSupplier) ibdfs.configure(cms);
    dis = new DefaultIBDataIngesterSupplier(wps, () -> log);
    dsi = new DefaultIBDataSetIdentifier();
    dsi.setDescription("desc");
    dsi.setName("name");
    dsi.setPath(f.toString());
    dsi.injectGAV("X", "Y", "1.0");
    i = new Ingestion();
    i.setDataSet(dsi);
    dss = new TreeMap<>();
    dssFail = new TreeMap<>();
    dssPass = new TreeMap<>();
    DataSet dset = dsi.asDataSet();
    dset.setCreationDate(now);
    dset.setUuid(UUID.randomUUID().toString());
    dset.setMetadata(new Metadata());
    ibdataset = new DefaultIBDataSet(dset);
    dssm = new AbstractIBDataSourceSupplierMapper(log, t2e, wps) {

      @Override
      public IBDataSourceSupplier getSupplierFor(String temporaryId, IBDataStreamIdentifier v) {
        IBDataSource ibds = new DefaultTestingSource("dummy:source") {
          public List<IBChecksumPathType> get() {
            try {
              IBChecksumPathType reference = copyToTempChecksumAndPath(wps.get(), f);
              return Arrays.asList(reference);
            } catch (IOException e) {
              throw new IBDataException("Test failed", e);
            }
          }
        };
        return new DefaultIBDataSourceSupplier("X", ibds, getWorkingPath());
      }

      @Override
      public List<String> getHeaders() {
        return asList("dummy:");
      }

    };
    dssmPass = new AbstractIBDataSourceSupplierMapper(log, t2e, wps) {

      @Override
      public IBDataSourceSupplier getSupplierFor(String temporaryId, IBDataStreamIdentifier v) {
        IBDataSource ibds = new DefaultTestingSource("dummy:source") {
          public Optional<org.infrastructurebuilder.util.artifacts.Checksum> getChecksum() {
            return of(filesDotTxtChecksum);
          };

          public List<IBChecksumPathType> get() {
            try (InputStream source = newInputStream(f)) {
              IBChecksumPathType reference = copyToDeletedOnExitTempChecksumAndPath(wps.get(), "X", "Y", source);
              return Arrays.asList(reference);
            } catch (IOException e) {
              throw new IBDataException("Test failed", e);
            }
          }
        };
        return new DefaultIBDataSourceSupplier("X", ibds, getWorkingPath());
      }

      @Override
      public List<String> getHeaders() {
        return asList("dummy:");
      }

    };
    k = dssm.getSupplierFor(UUID.randomUUID().toString(), null); // Returning a dummy value no matter what
    dss.put("X", k);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testType() {
    IBDataSource ibds = new DefaultTestingSource("dummy:source");
    assertFalse(ibds.getMimeType().isPresent());
  }

  @Test
  public void testGet() throws IOException {
    IBDataSetFinalizer<Ingestion> finalizer = ibdfs.get();
    dis = dis.getConfiguredSupplier(cms);
    assertNotNull(dis);
    c = dis.get();// configure() call default returns itself
    List<Supplier<IBDataStream>> val = c.ingest(dss);

    IBChecksumPathType finalized = finalizer.finalize(ibdataset, i, val, Optional.empty());
    assertTrue(Files.isDirectory(finalized.getPath()));
    assertNotNull(val);
    assertEquals(1, val.size());
    IBDataStream q = val.get(0).get();
    assertEquals(TEXT_PLAIN, q.getMimeType());
  }

  @Test
  public void testPassChecksum() throws IOException {
    dssPass.put("X", dssmPass.getSupplierFor(UUID.randomUUID().toString(), null));
    dis = dis.getConfiguredSupplier(cms);
    assertNotNull(dis);
    c = dis.get();// configure() call default returns itself
    List<Supplier<IBDataStream>> val = c.ingest(dssPass);
    assertNotNull(val);
  }

}
