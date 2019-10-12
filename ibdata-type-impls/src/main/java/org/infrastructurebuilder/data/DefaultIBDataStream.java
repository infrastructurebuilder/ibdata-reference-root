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
import static org.infrastructurebuilder.data.IBDataException.cet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.Vector;
import java.util.function.Supplier;

import org.infrastructurebuilder.data.model.DataStream;
import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.files.BasicIBChecksumPathType;
import org.infrastructurebuilder.util.files.DefaultIBChecksumPathType;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;

public class DefaultIBDataStream extends DefaultIBDataStreamIdentifier implements IBDataStream {

  private final List<InputStream> createdInputStreamsForThisInstance = new Vector<>();

  private final Supplier<InputStream> ss;
  private final IBChecksumPathType cpt;
  private Checksum calculatedChecksum = null;

  //  public DefaultIBDataStream(IBDataStreamIdentifier ds, UUID id,  Date now, Supplier<InputStream> ins) {
  //    super(id, ds.getURL(), ds.getName(), ds.getDescription(), ds.getChecksum(), now, ds.getMetadataAsDocument(),
  //        ds.getMimeType(), Optional.ofNullable(ds.getPath()));
  //    this.ss = requireNonNull(ins);
  //  }

  public final static DefaultIBDataStream from(DataStream ds, Supplier<Path> pathToRoot) {
    return new DefaultIBDataStream(ds,
        new BasicIBChecksumPathType(pathToRoot.get().resolve(ds.getPath()), ds.getChecksum(), ds.getMimeType()));
  }

  public DefaultIBDataStream(IBDataStreamIdentifier identifier, Path ins) {
    super(identifier.getId(), identifier.getURL(), identifier.getName(), identifier.getDescription(),
        identifier.getChecksum(), identifier.getCreationDate(), identifier.getMetadataAsDocument(),
        identifier.getMimeType(), Optional.ofNullable(identifier.getPath()));
    this.cpt = new BasicIBChecksumPathType(Objects.requireNonNull(ins), identifier.getChecksum(),
        identifier.getMimeType());
    this.ss = this.cpt;
  }

  public DefaultIBDataStream(IBDataStreamIdentifier identifier, IBChecksumPathType ins) {
    super(identifier.getId(), identifier.getURL(), identifier.getName(), identifier.getDescription(),
        requireNonNull(ins).getChecksum(), identifier.getCreationDate(), identifier.getMetadataAsDocument(),
        requireNonNull(ins).getType(), Optional.ofNullable(identifier.getPath()));
    this.cpt = ins;
    this.calculatedChecksum = this.cpt.getChecksum();
    this.ss = requireNonNull(ins);
  }

  @Override
  public InputStream get() {
    InputStream str = cet.withReturningTranslation(() -> this.ss.get());
    createdInputStreamsForThisInstance.add(str);
    return str;

  }

  @Override
  public Checksum getChecksum() {
    if (calculatedChecksum == null) {
      try (InputStream ins = get()) {
        calculatedChecksum = new Checksum(ins);
      } catch (IOException e) {
        throw new IBDataException("Error calculating checksum for " + getId());
      }
    }
    return this.calculatedChecksum;
  }

  @Override
  public void close() throws Exception {
    this.createdInputStreamsForThisInstance.forEach(stream -> {
      cet.withTranslation(() -> stream.close());
    });
  }

  @Override
  public IBDataStream relocateTo(Path newWorkingPath, TypeToExtensionMapper t2e) {
    IBChecksumPathType newCpt;
    Path target;
    if (this.cpt != null) {
      newCpt = this.cpt;
    } else {
      Path temp = newWorkingPath.resolve(UUID.randomUUID().toString());
      Checksum d;
      try (InputStream ins = get(); OutputStream outs = Files.newOutputStream(temp, StandardOpenOption.CREATE_NEW)) {
        d = cet.withReturningTranslation(() -> IBUtils.copyAndDigest(ins, outs));
        String t = getMimeType();
        newCpt = t == null ? new BasicIBChecksumPathType(temp, d) : new BasicIBChecksumPathType(temp, d, t);
      } catch (IOException e) {
        throw new IBDataException(e);
      }
    }
    target = newWorkingPath
        .resolve(newCpt.getChecksum().asUUID().get().toString() + t2e.getExtensionForType(newCpt.getType()));
    return new DefaultIBDataStream(this, cet.withReturningTranslation(() -> newCpt.moveTo(target)));
  }

}
