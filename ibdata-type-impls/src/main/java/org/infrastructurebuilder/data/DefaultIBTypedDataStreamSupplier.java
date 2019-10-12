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
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.infrastructurebuilder.util.artifacts.Checksum;
import org.w3c.dom.Document;

public class DefaultIBTypedDataStreamSupplier<T> implements IBTypedDataStreamSupplier<T> {

  private final Iterator<T> iterable;
  private final boolean parallel;
  private final IBDataStream original;

  public DefaultIBTypedDataStreamSupplier(IBDataStream original, Iterator<T> iterable, boolean parallel) {
    this.original = requireNonNull(original);
    this.iterable = requireNonNull(iterable);
    this.parallel = parallel;
  }

  @Override
  public Stream<T> get() {
    return stream(spliteratorUnknownSize(requireNonNull(iterable), 0), parallel);
  }

  @Override
  public UUID getId() {
    return original.getId();
  }

  @Override
  public Optional<URL> getURL() {
    return original.getURL();
  }

  @Override
  public Checksum getChecksum() {
    return original.getChecksum();
  }

  @Override
  public Date getCreationDate() {
    return original.getCreationDate();
  }

  @Override
  public Document getMetadataAsDocument() {
    return original.getMetadataAsDocument();
  }

  @Override
  public Object getMetadata() {
    return original.getMetadata();
  }

  @Override
  public String getMimeType() {
    return original.getMimeType();
  }

  @Override
  public Optional<String> getName() {
    return original.getName();
  }

  @Override
  public Optional<String> getDescription() {
    return original.getDescription();
  }

  @Override
  public String getPath() {
    return original.getPath();
  }
}
