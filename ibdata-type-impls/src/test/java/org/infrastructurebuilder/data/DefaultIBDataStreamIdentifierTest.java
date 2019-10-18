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

import java.net.URL;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.infrastructurebuilder.IBConstants;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class DefaultIBDataStreamIdentifierTest {

  private static final String DEFAULT_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>";
  private IBDataSource ibds;
  private DefaultIBDataStreamIdentifier i;
  private UUID id;
  private Optional<URL> url;
  private Optional<String> name;
  private Optional<String> description;
  private Checksum checksum;
  private Date creationDate;
  private Document metadata;
  private String mimeType;
  private Optional<String> path;
  private DefaultIBDataStreamIdentifier i2;

  @Before
  public void setUp() throws Exception {
    ibds = null;
    id = UUID.randomUUID();
    url = of(new URL("https://www.google.com"));
    name = of("dsname");
    description = of("dsdescription");
    checksum = new Checksum();
    creationDate = new Date();
    metadata = IBMetadataUtils.emptyDocumentSupplier.get();
    mimeType = IBConstants.APPLICATION_OCTET_STREAM;
    path = of("./");
    i = new DefaultIBDataStreamIdentifier(id, url, name, description, checksum, creationDate, metadata, mimeType, path);
    i2 = new DefaultIBDataStreamIdentifier(i);
  }

  @Test
  public void testHashCode() {
    assertEquals(i.hashCode(), i.hashCode());
    assertEquals(i.hashCode(), i2.hashCode());
  }

  @Test
  public void testGetId() {
    assertEquals(id, i.getId());
  }

  @Test
  public void testGetURL() {
    assertEquals(url, i.getURL());
  }

  @Test
  public void testGetName() {
    assertEquals(name, i.getName());
  }

  @Test
  public void testGetDescription() {
    assertEquals(description, i.getDescription());
  }

  @Test
  public void testGetChecksum() {
    assertEquals(checksum, i.getChecksum());
  }

  @Test
  public void testGetCreationDate() {
    assertEquals(creationDate, i.getCreationDate());
  }

  @Test
  public void testGetMetadata() {
    assertEquals(DEFAULT_XML, IBMetadataUtils.stringifyDocument.apply(i.getMetadata()));
  }

  @Test
  public void testGetMimeType() {
    assertEquals(IBConstants.APPLICATION_OCTET_STREAM, i.getMimeType());
  }

  @Test
  public void testGetPath() {
    assertEquals(path.get(), i.getPath());
  }

}
