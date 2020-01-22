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

import static java.lang.String.format;
import static java.util.Collections.synchronizedSortedMap;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.data.IBDataException;
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
    SortedMap<String, IBDataSchemaIngestionConfig> schemaIngest = i.asSchemaIngestion();
    return i.getDataSet().getDataSchemas().stream()
        // Stream of DefaultIBDataSchemaIngestionConfig
        .map(sig -> {
          return ssMappers.stream()
              // Get the first responding mapper or die
              .filter(m -> m.respondsTo(sig)).findFirst()
              .orElseThrow(() -> new IBDataException("No data sources are available for " + sig.getTemporaryId()))
              // get supplier mapped to temporary id
              .getSupplierFor(sig);
        }).map(Optional::get)
        // To a tree map with no dupes, obvs
        .collect(
            // to a sorted map
            toMap(
                // Starting with the id key
                IBSchemaSourceSupplier::getId
                // getting "this one"
                , identity()
                // dupes cause a failure
                , (left, right) -> {
                  throw new IBDataException(format("Dupe id for %s and %s", left, right));
                }
                // Returns to a synchronized sorted map
                , () -> synchronizedSortedMap(new TreeMap<>())));
  }

  @Override
  public void close() throws Exception {
    for (IBSchemaSourceSupplierMapper ss : ssMappers) {
      try {
        ss.close();
      } catch (Throwable i) {
        // ignore
        log.error(format("Error closing of %s, ignored", ss.toString()), i);
      }
    }

  }

}
