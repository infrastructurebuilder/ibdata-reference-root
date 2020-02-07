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
import static org.infrastructurebuilder.data.IBDataException.cet;
import static org.infrastructurebuilder.util.IBUtils.nullSafeObjectToString;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Vector;
import java.util.function.Supplier;

import org.infrastructurebuilder.IBException;
import org.infrastructurebuilder.data.model.DataStream;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.files.DefaultIBResource;
import org.infrastructurebuilder.util.files.IBResource;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;

public class DefaultIBDataStream extends DefaultIBDataStreamIdentifier implements IBDataStream {

  private final List<InputStream> createdInputStreamsForThisInstance = new Vector<>();

  private final Supplier<InputStream> streamSupplier;
  private final IBResource cpt;
  private Checksum calculatedChecksum = null;

  public final static DefaultIBDataStream from(DataStream ds, Supplier<Path> pathToRoot) {
    return new DefaultIBDataStream(ds,
        new DefaultIBResource(pathToRoot.get().resolve(ds.getPath()), ds.getChecksum(), of(ds.getMimeType())));
  }

  public DefaultIBDataStream(IBDataStreamIdentifier identifier, Path ins) {
    this(identifier, ins, empty());
  }

  public DefaultIBDataStream(IBDataStreamIdentifier identifier, Path ins, Optional<IBDataStructuredDataMetadata> sdmd) {
    super(identifier.getId() // Identifier (often not valid)
        , identifier.getUrl() // Optional source url
        , identifier.getName() // Name
        , identifier.getDescription() // DEsc
        , identifier.getChecksum() // Checksum of original
        , identifier.getCreationDate() // Creation date (will be over-written later)
        , identifier.getMetadata() // Metadata xpp3
        , identifier.getMimeType() // mime type
        , ofNullable(identifier.getPath()) // Possible path
        , ofNullable(identifier.getOriginalLength()) // Original length
        , ofNullable(identifier.getOriginalRowCount())); // Original row count
    sdmd.ifPresent(s -> this.setStructuredDataMetadata(s));
    this.cpt = new DefaultIBResource(Objects.requireNonNull(ins), identifier.getChecksum(),
        of(identifier.getMimeType()));
    this.streamSupplier = this.cpt;
  }

  public DefaultIBDataStream(IBDataStreamIdentifier id, IBResource ibresource) {
    super(id.getId()                        // id
        , id.getUrl()                       // source
        , id.getName()                      // name
        , id.getDescription()               // desc
        , requireNonNull(ibresource).getChecksum()  // checksom
        , id.getCreationDate()              // Creation date
        , id.getMetadata()                  // metadta
        , requireNonNull(ibresource).getType()      // MIME
        , nullSafeObjectToString.apply(ofNullable(ibresource.getPath()).map(Path::getFileName).orElse(null)) // Path to item
        , ofNullable(id.getOriginalLength()) // Length
        , ofNullable(id.getOriginalRowCount())); // Row count
    this.cpt = ibresource;
    this.calculatedChecksum = this.cpt.getChecksum();
    this.streamSupplier = requireNonNull(ibresource);
  }

  @Override
  public InputStream get() {
    InputStream str = cet.withReturningTranslation(() -> this.streamSupplier.get());
    createdInputStreamsForThisInstance.add(str);
    this.creationDate = new Date();
    return str;

  }

  @Override
  public Checksum getChecksum() {
    if (calculatedChecksum == null) {
      try (InputStream ins = get()) {
        calculatedChecksum = new Checksum(ins);
      } catch (IOException | IBException e) {
        throw new IBDataException("Error calculating checksum for " + getId());
      }
    }
    return this.calculatedChecksum;
  }

  @Override
  public void close() throws Exception {
    this.createdInputStreamsForThisInstance.forEach(stream -> {
      try {
        stream.close();
      } catch (IOException e) {
        // Nothing can actually be done
      }
    });
  }

  @Override
  public IBDataStream relocateTo(Path newWorkingPath, TypeToExtensionMapper t2e) {
    IBResource newCpt = this.cpt;
    String newTargetName = newCpt.getChecksum().asUUID().get().toString() + t2e.getExtensionForType(newCpt.getType());
    Path target = newWorkingPath.resolve(newTargetName);
    return new DefaultIBDataStream(this, cet.withReturningTranslation(() -> newCpt.moveTo(target)));
  }

  @Override
  public Optional<Path> getPathAsPath() {
    return ofNullable(this.cpt).map(IBResource::getPath);

  }

}
