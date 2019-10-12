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

import static java.util.Optional.ofNullable;

import java.net.URL;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.infrastructurebuilder.IBConstants;
import org.infrastructurebuilder.data.IBDataStreamIdentifier;
import org.infrastructurebuilder.data.IBMetadataUtils;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.w3c.dom.Document;

public class DefaultIBDataStreamIdentifierConfigBean implements IBDataStreamIdentifier {

  private String temporaryId;
  private String name;
  private String sha512;
  private URL url;
  private XmlPlexusConfiguration metadata;
  private String mimeType;
  private String description;
  private String path;
  private Date creationDate;
  private UUID id;

  public void setId(String id) {
    this.temporaryId = id;
  }

  public String getTemporaryId() {
    return temporaryId;
  }

  @Override
  public Optional<URL> getURL() {
    return ofNullable(url);
  }

  @Override
  public Optional<String> getName() {
    return ofNullable(name);
  }

  @Override
  public Optional<String> getDescription() {
    return ofNullable(description);
  }

  @Override
  public Document getMetadata() {
    return IBMetadataUtils.fromXpp3Dom.apply(this.metadata);
  }

  @Override
  public String getMimeType() {
    return ofNullable(mimeType).orElse(IBConstants.APPLICATION_OCTET_STREAM);
  }

  @Override
  public Checksum getChecksum() {
    return (sha512 != null) ? new Checksum(sha512) : null;

  }

  @Override
  public Date getCreationDate() {
    return Optional.ofNullable(this.creationDate).orElse(new Date());
  }

  @Override
  public UUID getId() {
    return Optional.ofNullable(this.id).orElse(UUID.randomUUID());
  }

  public void setMetadata(XmlPlexusConfiguration metadata) {
    this.metadata = metadata;
  }

  @Override
  public String getPath() {
    return this.path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setUrl(URL url) {
    this.url = url;
  }

  public void setSha512(String checksum) {
    this.sha512 = checksum;
  }

}
