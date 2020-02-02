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
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;

import java.nio.file.Path;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.data.IBSchemaDAOSupplier;
import org.infrastructurebuilder.data.IBSchemaIngester;
import org.infrastructurebuilder.data.IBSchemaSourceSupplier;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.slf4j.Logger;

@Named(DefaultIBSchemaIngesterSupplier.NAME)
public class DefaultIBSchemaIngesterSupplier<T> extends AbstractIBSchemaIngesterSupplier<T> {

  public static final String NAME = "default";

  @Inject
  public DefaultIBSchemaIngesterSupplier(IBRuntimeUtils ibr) {
    this(ibr, null);
  }

  private DefaultIBSchemaIngesterSupplier(IBRuntimeUtils ibr, ConfigMapSupplier cms) {
    super(ibr, cms);
  }

  @Override
  final protected IBSchemaIngester getInstance(IBRuntimeUtils ibr, T in) {
    return new DefaultIBSchemaIngester(ibr, getConfig().get());
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public DefaultIBSchemaIngesterSupplier<T> getConfiguredSupplier(ConfigMapSupplier config) {
    return new DefaultIBSchemaIngesterSupplier(getRuntimeUtils(), config);
  }

  public final class DefaultIBSchemaIngester extends AbstractIBSchemaIngester {

    public DefaultIBSchemaIngester(IBRuntimeUtils ibr, ConfigMap config) {
      super(ibr, config);
    }

//            return source.get().stream()
//                .map(ibPathChecksumType -> toIBDataStreamSupplier(getWorkingPath(), source, ibPathChecksumType, now));
//          })
//          // Collect to an ordered list
//          .collect(toList());
//    }

    @Override
    public SortedSet<IBSchemaDAOSupplier> ingest(SortedMap<String, IBSchemaSourceSupplier> dss) {
      return requireNonNull(dss, "Map of IBDataSchemaIngestionConfig instances")
          // SortedMap values come out in order, because that's how it works
          .values().stream()
          // Get the IBDataSchemaIngestionConfig
          .map(sourceSupplier -> {
            return DefaultIBSchemaDAOSupplierBuilder.newInstance()
                // Capture this source
                .withSource(sourceSupplier.get())
                // Carry over config
                .withIngestionConfig(sourceSupplier.getIngestionConfig()).build();
//            return (IBSchemaDAOSupplier) null;
          })
          // Collect to an ordered list
          .collect(Collectors.toCollection(TreeSet::new));
    }

  }

}
