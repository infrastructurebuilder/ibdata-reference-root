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
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.infrastructurebuilder.data.IBMetadataUtils.emptyXpp3Supplier;

import java.net.URL;
import java.nio.file.Path;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.files.IBChecksumPathType;

public class DefaultIBDataStreamIdentifier implements IBDataStreamIdentifier {

  private final UUID id;
  private final Optional<String> url;
  private final Optional<String> name;
  private final Optional<String> description;
  private final Checksum checksum;
  private final Date creationDate;
  private final Metadata metadata;
  private final String path;
  private final String mimeType;
  private final String originalLength;
  private final String originalRowCount;
  private IBDataStructuredDataMetadata structuredDataMetadata = null;
  private String temporaryId;

  public final static IBDataStreamSupplier toIBDataStreamSupplier(Path workingPath, IBDataSource source,
      IBChecksumPathType ibPathChecksumType, Date now) {
    String src = ibPathChecksumType.getSourceURL().map(URL::toExternalForm).orElse(source.getSourceURL());
    Path localPath = ibPathChecksumType.getPath();
    String size = ibPathChecksumType.size().toString();
    String p = requireNonNull(workingPath).relativize(localPath).toString();
    Checksum c = ibPathChecksumType.getChecksum();
    UUID id = c.asUUID().get();

    DefaultIBDataStreamIdentifier ddsi = new DefaultIBDataStreamIdentifier(id
    // Created URL
        , of(src)
        // Name
        , source.getName()
        //
        , source.getDescription()
        //
        , ibPathChecksumType.getChecksum()
        //
        , now
        //
        , source.getMetadata().orElse(emptyXpp3Supplier.get())
        //
        , ibPathChecksumType.getType()
        //
        , of(p)
        // Size
        , of(size)
        // rows
        , empty());

    return new DefaultIBDataStreamSupplier(new DefaultIBDataStream(ddsi, ibPathChecksumType));

  };

  public DefaultIBDataStreamIdentifier(UUID id, Optional<String> url, Optional<String> name, Optional<String> description,
      Checksum checksum, Date creationDate, Metadata metadata, String mimeType, Optional<String> path, Optional<String> oLength,
      Optional<String> oRowCount) {
    this.id = id;
    this.url = requireNonNull(url);
    this.name = requireNonNull(name);
    this.description = requireNonNull(description);
    this.checksum = requireNonNull(checksum);
    this.creationDate = requireNonNull(creationDate);
    this.metadata = requireNonNull(metadata);
    this.mimeType = requireNonNull(mimeType);
    this.path = requireNonNull(path).orElse(null);
    this.originalLength = requireNonNull(oLength).orElse(null);
    this.originalRowCount = requireNonNull(oRowCount).orElse(null);
  }

  public DefaultIBDataStreamIdentifier(IBDataStreamIdentifier ds) {
    this(ds.getId(), ds.getUrl(), ds.getName(), ds.getDescription(), ds.getChecksum(), ds.getCreationDate(),
        ds.getMetadata(), ds.getMimeType(), ofNullable(ds.getPath()), ofNullable(ds.getOriginalLength()), ofNullable(ds.getOriginalRowCount()));
  }

  @Override
  public UUID getId() {
    return this.id;
  }

  @Override
  public Optional<String> getUrl() {

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
  public Metadata getMetadata() {

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
  public String getSha512() {
    return getChecksum().toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(checksum, creationDate, description, id, metadata, mimeType, name, originalLength,
        originalRowCount, path, structuredDataMetadata, url);
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
    return Objects.equals(checksum, other.checksum)
        && Objects.equals(creationDate, other.creationDate)
        && Objects.equals(description, other.description)
        && Objects.equals(id, other.id)
        && Objects.equals(metadata, other.metadata)
        && Objects.equals(mimeType, other.mimeType)
        && Objects.equals(name, other.name)
        && Objects.equals(originalLength, other.originalLength)
        && Objects.equals(originalRowCount, other.originalRowCount)
        && Objects.equals(path, other.path)
        && Objects.equals(structuredDataMetadata, other.structuredDataMetadata)
        && Objects.equals(url, other.url);
  }


  @Override
  public Optional<IBDataStructuredDataMetadata> getStructuredDataMetadata() {
    return ofNullable(this.structuredDataMetadata );
  }

  public void setStructuredDataMetadata(IBDataStructuredDataMetadata structuredDataMetadata) {
    this.structuredDataMetadata = structuredDataMetadata;
  }

  @Override
  public String getOriginalLength() {
    return this.originalLength;
  }

  @Override
  public String getOriginalRowCount() {
    return this.originalRowCount;
  }

  @Override
  public Optional<UUID> getReferencedSchemaId() {
    return empty();
  }

  @Override
  public Optional<IBDataProvenance> getProvenance() {
    return empty();
  }

  @Override
  public Optional<String> getTemporaryId() {
    return ofNullable(this.temporaryId);
  }

  public void setTemporaryId(String temporaryId) {
    this.temporaryId = temporaryId;
  }
}
