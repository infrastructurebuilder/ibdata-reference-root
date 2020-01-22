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

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.infrastructurebuilder.data.DefaultIBDataStream;
import org.infrastructurebuilder.data.DefaultIBDataStreamIdentifier;
import org.infrastructurebuilder.data.DefaultIBDataStreamSupplier;
import org.infrastructurebuilder.data.IBDataStreamSupplier;
import org.infrastructurebuilder.data.IBSchemaDAO;
import org.infrastructurebuilder.data.IBSchemaDAOSupplier;
import org.infrastructurebuilder.data.IBSchemaSource;
import org.infrastructurebuilder.data.Metadata;

public final class DefaultIBSchemaDAOSupplierBuilder implements IBSchemaDAOSupplierBuilder {
  public static IBSchemaDAOSupplierBuilder newInstance() {
    return new DefaultIBSchemaDAOSupplierBuilder();
  }

  private final IBSchemaSource<?> src;
  private final IBDataSchemaIngestionConfig config;

  private DefaultIBSchemaDAOSupplierBuilder() {
    this.src = null;
    this.config = null;
  }

  @Override
  public IBSchemaDAOSupplierBuilder withIngestionConfig(IBDataSchemaIngestionConfig c) {
    return new DefaultIBSchemaDAOSupplierBuilder(src, c);
  }

  @Override
  public IBSchemaDAOSupplierBuilder withSource(IBSchemaSource<?> src) {
    return new DefaultIBSchemaDAOSupplierBuilder(src, this.config);
  }

  private DefaultIBSchemaDAOSupplierBuilder(IBSchemaSource<?> src, IBDataSchemaIngestionConfig config) {
    this.src = src;
    this.config = config;
  }

  @Override
  public final IBSchemaDAOSupplier build() {

    Map<String, IBDataStreamSupplier> map = src.get().entrySet().parallelStream()
        .collect(toMap(e -> e.getKey(), v ->
        // Create a new supplier from the available data
        new DefaultIBDataStreamSupplier(
            // Stream
            new DefaultIBDataStream(
                // Identity
                new DefaultIBDataStreamIdentifier(
                    // Not intiially required
                    null
                    // No URL by default
                    , empty()
                    // Name
                    , src.getName()
                    // desc
                    , src.getDescription()
                    // Checksum of the file data
                    , v.getValue().getChecksum(), new Date()
                    // Metadata or default value
                    , src.getMetadata().orElse(new Metadata())
                    // Ascribed Mime type
                    , v.getValue().getType()
                    // No path, length or row count
                    , empty(), empty(), empty())
                // data
                , v.getValue()))));
    return new InternalIBSchemaDAOSupplier(config.getTemporaryId(), new InternalIBSchemaDAO(map, src));
  }

  private class InternalIBSchemaDAOSupplier implements IBSchemaDAOSupplier {

    private final String tempId;
    private final IBSchemaDAO dao;

    public InternalIBSchemaDAOSupplier(String tempId, IBSchemaDAO dao) {
      this.tempId = tempId;
      this.dao = Objects.requireNonNull(dao);
    }

    @Override
    public IBSchemaDAO get() {
      return dao;
    }

    @Override
    public String getTemporaryId() {
      return tempId;
    }

  }

  private class InternalIBSchemaDAO implements IBSchemaDAO {

    private final Map<String, IBDataStreamSupplier> map;
    private final IBSchemaSource<?> source;

    public InternalIBSchemaDAO(Map<String, IBDataStreamSupplier> map, IBSchemaSource<?> src) {
      this.map = requireNonNull(map, "Data stream suppliers");
      this.source = requireNonNull(src, "IBSchemaSource");
    }

    @Override
    public Map<String, IBDataStreamSupplier> get() {
      return unmodifiableMap(map);
    }

    @Override
    public Optional<IBSchemaSource<?>> getSource() {
      return ofNullable(source);
    }

  }
}
