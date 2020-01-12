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

import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;

import java.nio.file.Path;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBIngestedSchemaSupplier;
import org.infrastructurebuilder.data.IBSchemaSourceSupplier;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;
import org.slf4j.Logger;

@Named
public class DefaultIBSchemaSourceSupplierFactory implements IBSchemaSourceSupplierFactory {

  private final TypeToExtensionMapper mapper;
  private final Logger log;
  private final List<IBSchemaSourceSupplierMapper> ssMappers;
  private final PathSupplier workingPathSupplier;

  @Inject
  public DefaultIBSchemaSourceSupplierFactory(LoggerSupplier l, TypeToExtensionMapper t2e,
      List<IBSchemaSourceSupplierMapper> dssMappers,
      @Named(IBDATA_WORKING_PATH_SUPPLIER) PathSupplier workingPathSupplier) {
    this.log = requireNonNull(l).get();
    this.mapper = requireNonNull(t2e);
    this.ssMappers = requireNonNull(dssMappers);
    this.workingPathSupplier = requireNonNull(workingPathSupplier);
  }

  @Override
  public Path getTargetPath() {
    return this.workingPathSupplier.get();
  }

  @Override
  public final SortedMap<String, IBSchemaSourceSupplier> mapIngestionToSuppliers(IBIngestion i) {
    SortedMap<String, IBDataSchemaIngestionConfig> ingestionConfiug = i.asSchemaIngestion();
    List<IBIngestedSchemaSupplier> k = i.getDataSet().getDataSchemas().stream().map(dStream -> {
      IBSchemaSourceSupplierMapper first = ssMappers.stream().filter(m -> m.respondsTo(dStream)).findFirst()
          .orElseThrow(() -> new IBDataException("No data sources are available for " + dStream.getTemporaryId()));
      return first.getSupplierFor(dStream.getTemporaryId(), dStream);
    }).collect(Collectors.toList());
    ;
    SortedMap<String, IBIngestedSchemaSupplier> q = k.stream()
        .collect(Collectors.toMap(IBIngestedSchemaSupplier::getTemporaryId, identity(), (prev, now) -> now, TreeMap::new));
    return null;
  }

}
