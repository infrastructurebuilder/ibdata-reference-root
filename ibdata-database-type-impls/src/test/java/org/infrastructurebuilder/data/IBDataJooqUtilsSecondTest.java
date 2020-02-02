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

import static org.junit.Assert.assertNotNull;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.MapProxyGenericData;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.infrastructurebuilder.IBConstants;
import org.infrastructurebuilder.data.JooqAvroRecordWriterSupplier.JooqRecordWriter;
import org.infrastructurebuilder.data.ingest.DefaultIBDataStreamIdentifierConfigBean;
import org.infrastructurebuilder.data.model.PersistedIBSchema;
import org.infrastructurebuilder.data.model.io.xpp3.PersistedIBSchemaXpp3Writer;
import org.infrastructurebuilder.data.util.files.DefaultTypeToExtensionMapper;
import org.infrastructurebuilder.util.BasicCredentials;
import org.infrastructurebuilder.util.DefaultBasicCredentials;
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
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IBDataJooqUtilsSecondTest {

  private static final String SELECT_STRING = "SELECT * FROM TEST ORDER BY ID;";

  public final static Logger log = LoggerFactory.getLogger(IBDataJooqUtilsSecondTest.class);

  private static TestingPathSupplier wps = new TestingPathSupplier();
  private final static IBRuntimeUtils ibr = new IBRuntimeUtilsTesting(wps, log,
      new DefaultGAV(new FakeIBVersionsSupplier()), new FakeCredentialsFactory(), new IBArtifactVersionMapper() {
      });


  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    wps.finalize();
  }

  private ConfigMap c;

  private TypeToExtensionMapper t2e;

  private String theUrl;

  private DefaultIBDataStreamIdentifierConfigBean b;

  private IBDataSourceSupplier<String> s;

  private IBDataSource<String> ds;

  private Connection conn;

  private IBResource read;

  private GenericData jrmpGD = new MapProxyGenericData(new Formatters());

  private Path targetPath;

  private PersistedIBSchema ibSchema;

  private Result<Record> result;

  private Record firstRecord;

  private JooqAvroRecordWriterSupplier w2;

  private GenericDataSupplier gds;

  private DefaultIBDataAvroUtilsSupplier aus;

  private JooqRecordWriter w;

  private ConfigMap configMap;

  public static Logger getLog() {
    return log;
  }

  @Before
  public void setUp() throws Exception {
    targetPath = wps.get();
    c = new ConfigMap();
    c.put("dialect", "H2");
    theUrl = "jdbc:h2:" + wps.getTestClasses().resolve("test").toAbsolutePath().toString();
    c.put("url", theUrl);
    c.put("query", SELECT_STRING);
    // c.put(IBDataSource.TARGET_PATH, wps.get());
    c.put(IBDataConstants.DATE_FORMATTER, "yyyy-MM-dd");
    t2e = new DefaultTypeToExtensionMapper();
    b = new DefaultIBDataStreamIdentifierConfigBean();
    b.setDescription("desc");
    b.setId(UUID.randomUUID().toString());
    b.setMetadata(new XmlPlexusConfiguration("metadata"));
    b.setMimeType(IBConstants.TEXT_CSV); // obviously wrong
    b.setName("name");
    b.setPath(null);
    b.setSha512(null);
    b.setUrl(theUrl);
    if (conn == null) {
      String url = theUrl;
      BasicCredentials bc = new DefaultBasicCredentials("SA", Optional.empty());
      conn = IBDataException.cet
          .withReturningTranslation(() -> DriverManager.getConnection(url, bc.getKeyId(), bc.getSecret().orElse(null)));

    }
    SQLDialect dialect = SQLDialect.H2;
    String recordName = "test";
    Optional<String> sString;
    DSLContext create = DSL.using(conn, dialect);
    final Result<Record> firstResult = create.fetch(SELECT_STRING);
    String namespace = "org.test";

    ibSchema = (PersistedIBSchema) IBDataJooqUtils.ibSchemaFromRecordResults(getLog(), namespace, recordName, "THE DOC",
        theUrl, firstResult, IbdataDatabaseTypeImplsVersioning.apiVersion());

    result = create.fetch(SELECT_STRING); // Read again if we had to create the schema
    getLog().info("Reading data from dataset");
    firstRecord = result.get(0); // First record
    Path schemaFile = wps.get().resolve(UUID.randomUUID().toString());
    try (Writer w = Files.newBufferedWriter(schemaFile)) {
      PersistedIBSchemaXpp3Writer writer = new PersistedIBSchemaXpp3Writer();
      writer.write(w, ibSchema);
    }
    gds = new DefaultGenericDataSupplier(ibr);
    aus = new DefaultIBDataAvroUtilsSupplier(ibr, gds);
  }

  @After
  public void tearDown() throws Exception {
    if (conn != null)
      conn.close();
  }

  @Test
  public void testGetFieldFromType() {
    configMap = new ConfigMap();
    configMap.put(JooqAvroRecordWriterSupplier.TARGET, targetPath);
    DefaultConfigMapSupplier cms = new DefaultConfigMapSupplier(configMap);
    IBDataAvroUtils local = aus.configure(cms).get();
    Optional<List<Schema>> alaschema = local.to(Arrays.asList(ibSchema.clone()));
    Schema ls = alaschema.get().get(0);
    configMap.put(JooqAvroRecordWriterSupplier.SCHEMA, ls);
    w2 = (JooqAvroRecordWriterSupplier) new JooqAvroRecordWriterSupplier(ibr, aus)
        .configure(cms);
    w = (JooqRecordWriter) w2.get();
    read = w.writeRecords(result);
    assertNotNull(read);
  }

}
