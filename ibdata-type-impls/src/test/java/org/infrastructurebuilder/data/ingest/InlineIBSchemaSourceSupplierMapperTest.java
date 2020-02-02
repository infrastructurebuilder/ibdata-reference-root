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

import java.util.Map;
import java.util.Optional;

import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.infrastructurebuilder.data.IBSchemaSource;
import org.infrastructurebuilder.data.IBSchemaSourceSupplier;
import org.infrastructurebuilder.util.FakeCredentialsFactory;
import org.infrastructurebuilder.util.artifacts.IBArtifactVersionMapper;
import org.infrastructurebuilder.util.artifacts.impl.DefaultGAV;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.DefaultConfigMapSupplier;
import org.infrastructurebuilder.util.config.FakeIBVersionsSupplier;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;
import org.infrastructurebuilder.util.config.IBRuntimeUtilsTesting;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.infrastructurebuilder.util.files.IBResource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InlineIBSchemaSourceSupplierMapperTest {
  public final static Logger log = LoggerFactory.getLogger(InlineIBSchemaSourceSupplierMapperTest.class);
  public final static TestingPathSupplier wps = new TestingPathSupplier();

  public final static IBRuntimeUtils ibr = new IBRuntimeUtilsTesting(wps, log);

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  private InlineIBSchemaSourceSupplierMapper a;
  private DefaultConfigMapSupplier cms;
  private DefaultIBDataSchemaIngestionConfigBean v,w;
  private XmlPlexusConfiguration inline;
  private ConfigMap config;

  @Before
  public void setUp() throws Exception {
    inline = new XmlPlexusConfiguration("inline");
    inline.addChild(new XmlPlexusConfiguration("schema"));
    config = new ConfigMap();

    cms = new DefaultConfigMapSupplier();
    a = new InlineIBSchemaSourceSupplierMapper(ibr);
    v = new DefaultIBDataSchemaIngestionConfigBean();
    w= new DefaultIBDataSchemaIngestionConfigBean();
    w.setMetadata(new XmlPlexusConfiguration("metadata"));
    w.setInline(inline);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testRespondsTo() {
    assertFalse(a.respondsTo(v));
  }

  @Test
  public void testGetSupplierFor() {
    Optional<IBSchemaSourceSupplier> c = a.getSupplierFor(w);
    assertTrue(c.isPresent());
    IBSchemaSourceSupplier vv = c.get();
    IBSchemaSource<?> src = vv.get();
    Map<String, IBResource> map = src.configure(w.asConfigMap()).get();
  }

}
