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

import static java.util.Objects.requireNonNull;
import static org.infrastructurebuilder.data.IBMetadataUtils.stringifyDocument;

import java.net.URL;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

import org.infrastructurebuilder.IBConstants;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.w3c.dom.Document;

public class DefaultIBDataStreamIdentifier implements IBDataStreamIdentifier {

  private final UUID id;
  private final Optional<URL> url;
  private final Optional<String> name;
  private final Optional<String> description;
  private final Checksum checksum;
  private final Date creationDate;
  private final Document metadata;
  private final String path;
  private final String mimeType;

  public DefaultIBDataStreamIdentifier(UUID id, Optional<URL> url, Optional<String> name, Optional<String> description,
      Checksum checksum, Date creationDate, Document metadata, String mimeType, Optional<String> path) {
    this.id = id;
    this.url = requireNonNull(url);
    this.name = requireNonNull(name);
    this.description = requireNonNull(description);
    this.checksum = requireNonNull(checksum);
    this.creationDate = requireNonNull(creationDate);
    this.metadata = requireNonNull(metadata);
    this.mimeType = requireNonNull(mimeType);
    this.path = requireNonNull(path).orElse(null);
  }

  public DefaultIBDataStreamIdentifier(IBDataStreamIdentifier ds) {
    this(ds.getId(), ds.getURL(), ds.getName(), ds.getDescription(), ds.getChecksum(), ds.getCreationDate(),
        ds.getMetadataAsDocument(), ds.getMimeType(), Optional.ofNullable(ds.getPath()));
  }

  public DefaultIBDataStreamIdentifier(IBDataSource source, Date creationDate, Optional<String> path) {
    this(UUID.randomUUID(), Optional.ofNullable(source.getSourceURL()), source.getName(), source.getDescription(),
        source.getChecksum().orElse(new Checksum()), creationDate,
        source.getMetadata().orElse(IBMetadataUtils.emptyDocumentSupplier.get()),
        source.getMimeType().orElse(IBConstants.APPLICATION_OCTET_STREAM), path);
  }

  @Override
  public UUID getId() {
    return this.id;
  }

  @Override
  public Optional<URL> getURL() {

    return this.url;
  }

  @Override
  public Optional<String> getName() {

    return this.name;
  }

  @Override
  public Optional<String> getDescription() {

    return this.description;
  }

  @Override
  public Checksum getChecksum() {
    return Objects.requireNonNull(this.checksum, "No checksum for DefaultIBDataSetreamIdentifier " + this.id);
  }

  @Override
  public Date getCreationDate() {

    return this.creationDate;
  }

  @Override
  public Document getMetadata() {

    return this.metadata;
  }

  @Override
  public String getMimeType() {

    return this.mimeType;
  }

  @Override
  public String getPath() {

    return this.path;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (checksum.hashCode());
    result = prime * result + (creationDate.hashCode());
    result = prime * result + (description.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + (metadata.hashCode());
    result = prime * result + (mimeType.hashCode());
    result = prime * result + (name.hashCode());
    result = prime * result + ((path == null) ? 0 : path.hashCode());
    result = prime * result + (url.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    DefaultIBDataStreamIdentifier other = (DefaultIBDataStreamIdentifier) obj;
    if (!checksum.equals(other.checksum))
      return false;
    if (!creationDate.equals(other.creationDate))
      return false;
    if (!description.equals(other.description))
      return false;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    if (!mimeType.equals(other.mimeType))
      return false;
    if (!name.equals(other.name))
      return false;
    if (path == null) {
      if (other.path != null)
        return false;
    } else if (!path.equals(other.path))
      return false;
    if (!url.equals(other.url))
      return false;
    if (!w3cDocumentEqualser.apply(metadata, other.metadata))
      return false;
    return true;
  }

  /**
   * FIXME Move this to IBMetadataUtils
   * Function to compare W3c Dcoument instances (by string compare, like a filthy animal
   */
  public final static BiFunction<Document, Document, Boolean> w3cDocumentEqualser = (lhs, rhs) -> {
    return stringifyDocument.apply(lhs).equals(stringifyDocument.apply(rhs));
  };
}
