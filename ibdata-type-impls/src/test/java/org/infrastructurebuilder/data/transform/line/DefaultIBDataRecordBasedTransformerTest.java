package org.infrastructurebuilder.data.transform.line;

import static org.junit.Assert.*;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.infrastructurebuilder.data.IBDataDataStreamRecordFinalizerSupplier;
import org.infrastructurebuilder.data.IBDataStreamRecordFinalizer;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultIBDataRecordBasedTransformerTest {

  private final static TestingPathSupplier wps = new TestingPathSupplier();
  private final static Logger log = LoggerFactory.getLogger(DefaultIBDataRecordBasedTransformerTest.class);
  private IBDataDataStreamRecordFinalizerSupplier<String> finalizerSupplier;
  private Map<String, IBDataRecordTransformerSupplier> rs;
  private Path p;
  private DefaultIBDataRecordBasedTransformer t;
  private Path thePath;

  @Before
  public void setUp() throws Exception {
    thePath = wps.get();
    rs = new HashMap<>();
    s1 = new DefaultTestIBDataRecordTransformerSupplierStringToString();
    finalizerSupplier = new StringIBDataStreamRecordFinalizerSupplier(() -> thePath);
    t = new DefaultIBDataRecordBasedTransformer(p, log, rs, finalizerSupplier.get());
  }

  @Test
  public void testConfigure() {
    fail("Not yet implemented");
  }

  @Test
  public void testGetHint() {
    assertEquals(DefaultIBDataRecordBasedTransformerSupplier.RECORD_BASED_TRANSFORMER_SUPPLIER, t.getHint());
  }

  @Test
  public void testTransform() {
    fail("Not yet implemented");
  }

  @Test
  public void testGetConfiguredTransformers() {
    fail("Not yet implemented");
  }

  @Test
  public void testGetDataLineSuppliers() {
    fail("Not yet implemented");
  }

  @Test
  public void testGetConfiguredFinalizer() {
    fail("Not yet implemented");
  }

  @Test
  public void testProcessStream() {
    fail("Not yet implemented");
  }

}
