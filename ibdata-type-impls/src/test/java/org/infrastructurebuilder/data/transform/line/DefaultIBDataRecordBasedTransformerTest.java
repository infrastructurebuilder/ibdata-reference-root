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

import static java.util.Optional.of;
import static org.infrastructurebuilder.data.IBMetadataUtils.RECORD_SPLITTER;
import static org.infrastructurebuilder.data.IBMetadataUtils.MAP_SPLITTER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.infrastructurebuilder.data.DefaultIBDataSet;
import org.infrastructurebuilder.data.DefaultIBDataStream;
import org.infrastructurebuilder.data.DefaultIBDataStreamIdentifier;
import org.infrastructurebuilder.data.IBDataDataStreamRecordFinalizerSupplier;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBDataSet;
import org.infrastructurebuilder.data.IBDataStream;
import org.infrastructurebuilder.data.IBDataStreamIdentifier;
import org.infrastructurebuilder.data.IBDataTransformationResult;
import org.infrastructurebuilder.data.IBMetadataUtils;
import org.infrastructurebuilder.data.model.DataSet;
import org.infrastructurebuilder.data.transform.Transformer;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.DefaultConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.infrastructurebuilder.util.files.IBCoreReadDetectResponse;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class DefaultIBDataRecordBasedTransformerTest {

  private final static TestingPathSupplier wps = new TestingPathSupplier();
  private final static Logger log = LoggerFactory.getLogger(DefaultIBDataRecordBasedTransformerTest.class);
  private IBDataDataStreamRecordFinalizerSupplier<String> finalizerSupplier;
  private Map<String, IBDataRecordTransformerSupplier> rs;
  private Path p;
  private DefaultIBDataRecordBasedTransformer t;
  private Path thePath;
  private ConfigMap cfg;
  private ConfigMapSupplier cms;
  private IBDataSet ds;
  private List<IBDataStream> suppliedStreams;
  private List<String> transformersList = Arrays.asList("test1", "test2");
  private final Date creationDate = new Date();
  private IBDataRecordTransformerSupplier test1;
  private IBDataRecordTransformerSupplier test2;
  private Transformer transformer;

  @Before
  public void setUp() throws Exception {
    StringJoiner sj = new StringJoiner(RECORD_SPLITTER);
    transformersList.forEach(t -> sj.add(t + MAP_SPLITTER + t));
    thePath = wps.get();
    rs = new HashMap<>();
    test1 = new DefaultTestingIBDataRecordTransformerSupplier(1, () -> thePath);
    test2 = new DefaultTestingIBDataRecordTransformerSupplier(2, () -> thePath);
    rs.put("test1", test1);
    rs.put("test2", test2);
    HashMap<String,Object> hm = new HashMap<>();
    hm.put(IBMetadataUtils.TRANSFORMERSLIST, sj.toString());
    cfg = new ConfigMap(hm);
    cms = new DefaultConfigMapSupplier().addConfiguration(cfg);
    //    s1 = new DefaultTestIBDataRecordTransformerSupplierStringToString();
    finalizerSupplier = new StringIBDataStreamRecordFinalizerSupplier(() -> thePath).config(cms);
    t = new DefaultIBDataRecordBasedTransformer(thePath, log, rs, finalizerSupplier.get());
    DataSet d1 = new DataSet();
    d1.setUuid(UUID.randomUUID().toString());
    d1.setGroupId("x");
    d1.setArtifactId("y");
    d1.setVersion("1.0.0-SNAPSHOT");
    d1.setDataSetName("a");
    d1.setDataSetDescription("desc");
    d1.setMetadata(new Xpp3Dom("metadata"));
    d1.setModelVersion("0.11");
    d1.setCreationDate(creationDate);
    ds = new DefaultIBDataSet(d1);

    transformer = new Transformer();

    suppliedStreams = new ArrayList<>();

    suppliedStreams.add(getStreamFromURL(getClass().getResource("/rick.jpg")));
    suppliedStreams.add(getStreamFromURL(getClass().getResource("/lines.txt")));

  }

  private IBDataStream getStreamFromURL(URL resource) throws Exception {
    IBChecksumPathType c = readPathTypeFromFile(resource);
    Document metadata = IBMetadataUtils.emptyDocumentSupplier.get();
    IBDataStreamIdentifier i = new DefaultIBDataStreamIdentifier(null, of(resource), of("abc"), of("desc"),
        c.getChecksum(), creationDate, metadata, c.getType(), of(c.getPath().relativize(thePath).toString()));
    return new DefaultIBDataStream(i, c);
  }

  private IBChecksumPathType readPathTypeFromFile(URL resource) throws Exception {
    try (InputStream in = resource.openStream()) {
      return IBCoreReadDetectResponse.copyToDeletedOnExitTempChecksumAndPath(of(thePath), "abc", "b", in);
    }
  }

  @Test
  public void testGetHint() {
    assertEquals(DefaultIBDataRecordBasedTransformerSupplier.RECORD_BASED_TRANSFORMER_SUPPLIER, t.getHint());
  }

  @Test
  public void testTransform() {
    IBDataTransformationResult q = t.configure(cfg).transform(transformer, ds, suppliedStreams, true);
    assertEquals(0, q.getErrors().size());

    // FIXME Test the actual output
  }

  public class DefaultTestingIBDataRecordTransformerSupplier extends AbstractIBDataRecordTransformerSupplier
      implements IBDataRecordTransformerSupplier {

    private int type;

    public DefaultTestingIBDataRecordTransformerSupplier(int type, PathSupplier wps) {
      super(wps, null);
      this.type = type;
    }

    private DefaultTestingIBDataRecordTransformerSupplier(int type, PathSupplier wps, ConfigMapSupplier cms) {
      super(wps, cms);
      this.type = type;
    }

    @Override
    public String getHint() {
      return "test" + type;
    }

    @Override
    public AbstractIBDataRecordTransformerSupplier configure(ConfigMapSupplier cms) {
      return new DefaultTestingIBDataRecordTransformerSupplier(type, getWps(), cms);
    }

    @Override
    protected IBDataRecordTransformer getUnconfiguredTransformerInstance(Path workingPath) {
      switch (type) {
      case 1:
        return new Test1(getWps().get(), getConfigSupplier().get());
      case 2:
        return new Test2(getWps().get(), getConfigSupplier().get());
      default:
        throw new IBDataException("Wrong type, moron");
      }
    }

    public class Test1 extends AbstractIBDataRecordTransformer<String, String> {

      protected Test1(Path ps, ConfigMap config) {
        super(ps, config);
        // TODO Auto-generated constructor stub
      }

      @Override
      public String getHint() {
        return "test1";
      }

      @Override
      public IBDataRecordTransformer<String, String> configure(ConfigMap cms) {
        return this;
      }

      @Override
      public String apply(String t) {
        return t.trim();
      }

    }

    public class Test2 extends AbstractIBDataRecordTransformer<String, String> {

      protected Test2(Path ps, ConfigMap config) {
        super(ps, config);
      }

      @Override
      public String getHint() {
        return "test1";
      }

      @Override
      public IBDataRecordTransformer<String, String> configure(ConfigMap cms) {
        return this;
      }

      @Override
      public String apply(String t) {
        return t.toUpperCase();
      }

    }

  }

}