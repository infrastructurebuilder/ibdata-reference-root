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
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.infrastructurebuilder.data.DefaultIBDataStreamIdentifier.toIBDataStreamSupplier;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;

import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.data.IBDataIngester;
import org.infrastructurebuilder.data.IBDataSourceSupplier;
import org.infrastructurebuilder.data.IBDataStreamSupplier;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.DefaultConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.slf4j.Logger;

@Named(DefaultIBDataIngesterSupplier.NAME)
public class DefaultIBDataIngesterSupplier extends AbstractIBDataIngesterSupplier<Object> {

  public static final String NAME = "default";

  @Inject
  public DefaultIBDataIngesterSupplier(@Named(IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps, LoggerSupplier log) {
    this(wps, log, null);
  }

  private DefaultIBDataIngesterSupplier(PathSupplier wps, LoggerSupplier log, ConfigMapSupplier cms) {
    super(wps, log, cms);
  }

  @Override
  protected IBDataIngester getInstance(Optional<Path> workingPath, Optional<Object> in) {
    return new DefaultIBDataIngester(workingPath.get(), getLog(), getConfig().get());
  }

  @Override
  public DefaultIBDataIngesterSupplier getConfiguredSupplier(ConfigMapSupplier config) {
    return new DefaultIBDataIngesterSupplier(getWps(), () -> getLog(), config);
  }

  public final class DefaultIBDataIngester extends AbstractIBDataIngester {

    public DefaultIBDataIngester(Path workingPath, Logger log, ConfigMap config) {
      super(workingPath, log, config);
    }

    @Override
    public List<IBDataStreamSupplier> ingest(SortedMap<String, IBDataSourceSupplier<?>> dssList) {
      Date now = new Date(); // Ok for "now" (Get it?)
      return requireNonNull(dssList, "List of IBDataSourceSupplier instances")
          // SortedMap values come out in order, because that's how it works
          .values().stream()
          // Get the IBDataSource
          .map(IBDataSourceSupplier::get)
          // Get CONFIGURED source
          .map(ds -> ds.configure(new DefaultConfigMapSupplier(getConfig()).get()))
          // map configured source to
          .flatMap(source -> {
            getLog().info(format("Mapping %s from %s", source.getId(), source.getSourceURL()));
            return source.get().stream()
                .map(ibPathChecksumType -> toIBDataStreamSupplier(getWorkingPath(), source, ibPathChecksumType, now));
          })
          // Collect to an ordered list
          .collect(toList());
    }

  }

}
