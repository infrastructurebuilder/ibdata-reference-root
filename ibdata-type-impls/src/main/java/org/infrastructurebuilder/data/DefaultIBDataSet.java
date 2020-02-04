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

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.infrastructurebuilder.data.model.DataSet;

public class DefaultIBDataSet extends AbstractIBDataSet {
  private static final long serialVersionUID = -1385333942182011178L;
  private final Map<UUID, IBDataStreamSupplier> streamSuppliers;

  public final static DefaultIBDataSet decorateAddingSuppliers(DataSet ds, Supplier<Path> pathToRoot) {
    Map<UUID, IBDataStreamSupplier> v = ds.getStreams().stream()
        .map(d -> new DefaultIBDataStreamSupplier(DefaultIBDataStream.from(d, pathToRoot)))
        .collect(Collectors.toMap(k -> k.get().getId(), Function.identity()));
    DefaultIBDataSet d = new DefaultIBDataSet(ds, v);
    d.setPath(pathToRoot.get());
    d.getSchemas().stream().forEach(schema -> {
        schema.getSchemaAssets().stream().forEach(asset-> {
          // TODO Setup the assets for the data set that I just read
        });
    });
    return d;
  }

  public DefaultIBDataSet(DataSet ds) {
    this(ds, emptyMap());
  }

  private DefaultIBDataSet(IBDataSetIdentifier ds, Map<UUID, IBDataStreamSupplier> streamSuppliers) {
    super(requireNonNull(ds).getUuid(), ds.getCreationDate(), ds.getMetadata(), ds.getName(), ds.getDescription(),
        ds.getLocalPath(), ds.getGroupId(), ds.getArtifactId(), ds.getVersion()

    );
    this.streamSuppliers = requireNonNull(streamSuppliers);
  }

  private DefaultIBDataSet(IBDataSet ds, Map<UUID, IBDataStreamSupplier> streamSuppliers) {
    super(requireNonNull(ds).getUuid(), ds.getCreationDate(), ds.getMetadata(), ds.getName(), ds.getDescription(),
        ds.getLocalPath(), ds.getGroupId(), ds.getArtifactId(), ds.getVersion());
    this.streamSuppliers = requireNonNull(streamSuppliers);
  }

  public DefaultIBDataSet(IBDataSet ds) {
    this(ds, ds.getStreamSuppliers().stream().collect(Collectors.toMap(k -> k.get().getId(), Function.identity())));
  }

  public IBDataSet withStreamSuppliers(Map<UUID, IBDataStreamSupplier> streamSuppliers) {
    return new DefaultIBDataSet(this, streamSuppliers);
  }

  @Override
  public List<IBDataStreamSupplier> getStreamSuppliers() {
    return this.streamSuppliers.values().stream().collect(toList());
  }

}
