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
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.infrastructurebuilder.util.files.DefaultIBResource.copyToDeletedOnExitTempChecksumAndPath;
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

import org.infrastructurebuilder.data.DefaultIBDataSet;
import org.infrastructurebuilder.data.DefaultTestingSource;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBDataIngester;
import org.infrastructurebuilder.data.IBDataIngesterSupplier;
import org.infrastructurebuilder.data.IBDataSet;
import org.infrastructurebuilder.data.IBDataSource;
import org.infrastructurebuilder.data.IBDataSourceSupplier;
import org.infrastructurebuilder.data.IBDataStreamIdentifier;
import org.infrastructurebuilder.data.Metadata;
import org.infrastructurebuilder.data.model.DataSet;
import org.infrastructurebuilder.data.model.DataStream;
import org.infrastructurebuilder.data.util.files.DefaultTypeToExtensionMapper;
import org.infrastructurebuilder.util.CredentialsFactory;
import org.infrastructurebuilder.util.FakeCredentialsFactory;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.artifacts.IBArtifactVersionMapper;
import org.infrastructurebuilder.util.artifacts.impl.DefaultGAV;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.DefaultConfigMapSupplier;
import org.infrastructurebuilder.util.config.FakeIBVersionsSupplier;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;
import org.infrastructurebuilder.util.config.IBRuntimeUtilsTesting;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.infrastructurebuilder.util.files.IBResource;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

public class AbstractIBDataSourceSupplierMapperTest {
  private static final String DUMMY = "dummy:";
  public final static Logger log = getLogger(AbstractIBDataSourceSupplierMapperTest.class);
  public final static TestingPathSupplier wps = new TestingPathSupplier();
  private final static IBRuntimeUtils ibr = new IBRuntimeUtilsTesting(wps, log,
      new DefaultGAV(new FakeIBVersionsSupplier()), new FakeCredentialsFactory(), new IBArtifactVersionMapper() {
      });

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    wps.finalize();
  }

  private IBDataIngesterSupplier<String> dis;
  private ConfigMap configMap;
  private DefaultConfigMapSupplier cms;
  private IBDataIngester c;
  private SortedMap<String, IBDataSourceSupplier<?>> dss, dssFail, dssPass;
  private DefaultIBDataSetIdentifier dsi;
  private IBIngestion i;
  private TypeToExtensionMapper t2e = new DefaultTypeToExtensionMapper();
  private IBDataSourceSupplier<?> k;
  private IBDataSet ibdataset;
  private Date now = new Date();
  private Checksum filesDotTxtChecksum;
  private AbstractIBDataSourceSupplierMapper<String> dssm, dssmPass, dssmFail;
  private DataStream random;
  private CredentialsFactory cf;

  @Before
  public void setUp() throws Exception {
    Path f = wps.getTestClasses().resolve("lines.txt").toAbsolutePath();
    filesDotTxtChecksum = new Checksum(f);
    Path cache = wps.get().toAbsolutePath();
    configMap = new ConfigMap();
    // configMap.put(CACHE_DIRECTORY_CONFIG_ITEM, cache.toString());
    cms = new DefaultConfigMapSupplier(configMap);
    // ibdfs = new DefaultIBDataSetIngestionFinalizerSupplier(wps, () -> log, t2e);
    // ibdfs = (DefaultIBDataSetIngestionFinalizerSupplier) ibdfs.configure(cms);
    // dis = new DefaultIBDataIngesterSupplier(wps, () -> log);
    dsi = new DefaultIBDataSetIdentifier();
    dsi.setDescription("desc");
    dsi.setName("name");
    dsi.setPath(f);
    dsi.injectGAV("X", "Y", "1.0");
    i = new FakeIBIngestion(dsi);
    dss = new TreeMap<>();
    dssFail = new TreeMap<>();
    dssPass = new TreeMap<>();
    DataSet dset = dsi.asDataSet();
    dset.setCreationDate(now);
    dset.setUuid(UUID.randomUUID().toString());
    dset.setMetadata(new Metadata());
    ibdataset = new DefaultIBDataSet(dset);
    random = new DataStream();
    random.setTemporaryId(UUID.randomUUID().toString());
    cf = new FakeCredentialsFactory();
    dssm = new AbstractIBDataSourceSupplierMapper<String>(ibr) {

      @Override
      public IBDataSourceSupplier<String> getSupplierFor(IBDataStreamIdentifier v) {
        IBDataSource<String> ibds = new DefaultTestingSource("dummy:source") {
          public List<IBResource> get() {
            try (InputStream source = newInputStream(f)) {
              IBResource reference = copyToDeletedOnExitTempChecksumAndPath(wps.get(), "X", "Y", source);
              return Arrays.asList(reference);
            } catch (IOException e) {
              throw new IBDataException("Test failed", e);
            }
          }
        };
        return new DefaultIBDataSourceSupplier<String>("X", ibds, getWorkingPathSupplier(), empty());
      }

      @Override
      public List<String> getHeaders() {
        return asList(DUMMY);
      }

    };
    dssmFail = new AbstractIBDataSourceSupplierMapper<String>(ibr) {

      @Override
      public IBDataSourceSupplier<String> getSupplierFor(IBDataStreamIdentifier v) {
        IBDataSource<String> ibds = new DefaultTestingSource("dummy:source") {
          public Optional<org.infrastructurebuilder.util.artifacts.Checksum> getChecksum() {
            return of(new Checksum("ABCD"));
          };

          public List<IBResource> get() {
            try (InputStream source = newInputStream(f)) {
              IBResource reference = copyToDeletedOnExitTempChecksumAndPath(wps.get(), "X", "Y", source);
              return Arrays.asList(reference);
            } catch (IOException e) {
              throw new IBDataException("Test failed", e);
            }
          }
        };
        return new DefaultIBDataSourceSupplier<String>("X", ibds, getWorkingPathSupplier(), empty());
      }

      @Override
      public List<String> getHeaders() {
        return asList(DUMMY);
      }

    };
    dssmPass = new AbstractIBDataSourceSupplierMapper<String>(ibr) {

      @Override
      public IBDataSourceSupplier<String> getSupplierFor(IBDataStreamIdentifier v) {
        IBDataSource<String> ibds = new DefaultTestingSource("dummy:source") {
          public Optional<org.infrastructurebuilder.util.artifacts.Checksum> getChecksum() {
            return of(filesDotTxtChecksum);
          };

          public List<IBResource> get() {
            try (InputStream source = newInputStream(f)) {
              IBResource reference = copyToDeletedOnExitTempChecksumAndPath(wps.get(), "X", "Y", source);
              return Arrays.asList(reference);
            } catch (IOException e) {
              throw new IBDataException("Test failed", e);
            }
          }
        };
        return new DefaultIBDataSourceSupplier<String>("X", ibds, getWorkingPathSupplier(), empty());
      }

      @Override
      public List<String> getHeaders() {
        return asList(DUMMY);
      }

    };
    k = dssm.getSupplierFor(random); // Returning a dummy value no matter what
    dss.put("X", k);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testAccessors() {
    assertNotNull(dssm.getMapper());
    assertTrue(dssm.getHeaders().contains(DUMMY));
    assertEquals(1, dssm.getHeaders().size());
    dssm.getLog().info("Logging");
    assertFalse(dssm.respondsTo(null));
    DefaultIBDataStreamIdentifierConfigBean v = new DefaultIBDataStreamIdentifierConfigBean();
    v.setUrl("dummy:something");
    assertTrue(dssm.respondsTo(v));

  }

  @Test
  public void testGet() throws IOException {
    IBDataSourceSupplier<?> supplier = dssm.getSupplierFor(random);
    IBDataSource<?> source = supplier.get();

    IBResource unfinalized = source.get().get(0);
    assertTrue(Files.isRegularFile(unfinalized.getPath()));
  }

}
