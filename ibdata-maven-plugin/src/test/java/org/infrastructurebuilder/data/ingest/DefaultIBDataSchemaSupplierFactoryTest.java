package org.infrastructurebuilder.data.ingest;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;

import org.infrastructurebuilder.data.IBDataSchemaSupplier;
import org.infrastructurebuilder.data.util.files.DefaultTypeToExtensionMapper;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultIBDataSchemaSupplierFactoryTest {
  private final static Logger log = LoggerFactory.getLogger(DefaultIBDataSchemaSupplierFactoryTest.class);

  private static TestingPathSupplier wps = new TestingPathSupplier();

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    wps.finalize();
  }

  private List<IBDataSchemaSupplierMapper> dssMappers;
  private IBDataSchemaSupplierMapper a, b;
  private DefaultIBDataSchemaSupplierFactory d;
  private Ingestion i;

  private TypeToExtensionMapper t2e = new DefaultTypeToExtensionMapper();

  @Before
  public void setUp() throws Exception {
    a = new DefaultIBDataSchemaSupplierMapper(() -> log, t2e, wps);
    b = new DefaultIBDataSchemaSupplierMapper(() -> log, t2e, wps);
    dssMappers = Arrays.asList(a, b);
    d = new DefaultIBDataSchemaSupplierFactory(() -> log, t2e, dssMappers, wps);
    i = new Ingestion();
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testMapIngestionToSuppliers() {
    SortedMap<String, IBDataSchemaSupplier> val = d.mapIngestionToSuppliers(i);
    assertEquals(2, val.size());
  }

}
