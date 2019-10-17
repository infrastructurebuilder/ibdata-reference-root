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

import static java.util.Optional.of;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.infrastructurebuilder.IBException;
import org.infrastructurebuilder.data.model.DataStream;
import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.infrastructurebuilder.util.files.TestThrowingIBChecksumType;
import org.infrastructurebuilder.util.files.TestThrowingInputStream;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class DefaultIBDataStreamTest {

  private static final String JPG = "image/jpeg";
  private static final String DESC = "Rickrolled";
  private static final String NAME = "name";

  /**
   * Move to IBUtils in next release
   * @param ins
   * @param target
   * @return
   */
  public static Checksum copyAndDigest(final InputStream ins, final Path target) {
    try (OutputStream outs = Files.newOutputStream(target, StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
      return IBUtils.copyAndDigest(ins, outs);
    } catch (IOException | NoSuchAlgorithmException e) {
      throw new IBException(e);
    }
  }

  private Path path;
  private DefaultIBDataStreamIdentifier identifier;
  private DefaultIBDataStream ib1;
  private Checksum checksum;
  private Document metadata;
  private String mimeType;
  private InputStream rick;
  private Path p1;
  private DefaultIBDataStream ib2;
  private DataStream ds;
  private final static Date now = new Date();
  private static TestingPathSupplier wps = new TestingPathSupplier();

  @Before
  public void setUp() throws Exception {
    p1 = wps.get();
    path = p1.resolve(UUID.randomUUID().toString() + ".jpg");
    metadata = IBMetadataUtils.emptyDocumentSupplier.get();
    mimeType = JPG;
    rick = getClass().getResourceAsStream("/rick.jpg");
    checksum = copyAndDigest(rick, path);
    identifier = new DefaultIBDataStreamIdentifier(checksum.asUUID().get(), of(p1.toUri().toURL()), of(NAME), of(DESC),
        checksum, now, metadata, mimeType, of(path.toString()));
    ib1 = new DefaultIBDataStream(identifier, path);
    ib2 = new DefaultIBDataStream(identifier, new TestThrowingIBChecksumType());

    ds = new DataStream();
    ds.setPath(path.relativize(p1).toString());
    ds.setSha512("abcd");
    ds.setUuid(UUID.randomUUID().toString());
    ds.setMimeType(JPG);
    ds.setCreationDate(now);
  }

  @Test(expected = IBException.class)
  public void testCopyAndDigest() {
    copyAndDigest(new TestThrowingInputStream(IOException.class), path);
  }

  //  @Test
  //  public void testDataStreamIdentifierConstructor2() {
  //    IBDataSource source = null;
  //    assertNotNull(new DefaultIBDataStreamIdentifier(source, now, of(path.toString())));
  //  }

  @Test
  public void testDataStreamIdentifierCopyConstructor() {
    assertNotNull(new DefaultIBDataStreamIdentifier(identifier));
  }

  @Test
  public void testGetChecksum() {
    assertEquals(checksum, ib1.getChecksum());
    assertEquals(checksum, ib1.getChecksum()); // Twice for cached copy
  }

  @Test(expected = IBDataException.class)
  public void testGetChecksumWithFailingInputStream() {
    Supplier<InputStream> ins = () -> new TestThrowingInputStream(IOException.class);
    Reflect.on(ib2).set("ss", ins).set("calculatedChecksum", null);
    ib2.getChecksum(); // Setup to fail
  }

  @Test
  public void testGet() {
    Path p2 = p1.resolve(UUID.randomUUID().toString());
    Checksum d = copyAndDigest(ib1.get(), p2);
    assertEquals(checksum, d);
  }

  @Test
  public void testClose() throws Exception {
    List<InputStream> s = Reflect.on(ib1).get("createdInputStreamsForThisInstance");
    s.add(new TestThrowingInputStream(IOException.class));
    ib1.close();
  }

  @Test
  public void testRelocateTo() throws Exception {
    Path p2 = wps.get();
    IBDataStream v = ib1.relocateTo(p2, new DefaultTypeToExtensionMapper());
    assertNotNull(v);
  }

  @Test
  public void testGetId() {
    assertEquals(checksum.asUUID().get(), ib1.getId());
  }

  @Test
  public void testGetURL() throws MalformedURLException {
    assertTrue(ib1.getURL().get().toExternalForm().startsWith(p1.toUri().toURL().toExternalForm()));
  }

  @Test
  public void testGetName() {
    assertEquals(NAME, ib1.getName().get());
  }

  @Test
  public void testGetDescription() {
    assertEquals(DESC, ib1.getDescription().get());
  }

  @Test
  public void testGetCreationDate() {
    assertEquals(now, ib1.getCreationDate());
  }

  @Test
  public void testGetMetadata() {
    assertEquals(metadata, ib1.getMetadata());
  }

  @Test
  public void testGetMimeType() {
    assertEquals(JPG, ib1.getMimeType());
  }

  @Test
  public void testGetPath() {
    assertEquals(path.toString(), ib1.getPath());
  }

  @Test
  public void testFrom() {
    DefaultIBDataStream v = DefaultIBDataStream.from(ds, () -> path);
  }

}