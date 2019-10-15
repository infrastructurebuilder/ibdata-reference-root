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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.data.AbstractIBDataSetFinalizer;
import org.infrastructurebuilder.data.AbstractIBDataSetFinalizerSupplier;
import org.infrastructurebuilder.data.IBDataModelUtils;
import org.infrastructurebuilder.data.IBDataSet;
import org.infrastructurebuilder.data.IBDataSetFinalizer;
import org.infrastructurebuilder.data.IBDataSetFinalizerSupplier;
import org.infrastructurebuilder.data.IBDataStreamSupplier;
import org.infrastructurebuilder.data.IBMetadataUtils;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("default-ingest")
public class DefaultIBDataSetJDBCIngestionFinalizerSupplier extends AbstractIBDataSetFinalizerSupplier {

  public final static Logger logger = LoggerFactory.getLogger(DefaultIBDataSetJDBCIngestionFinalizerSupplier.class);

  @Inject
  public DefaultIBDataSetJDBCIngestionFinalizerSupplier(LoggerSupplier l, TypeToExtensionMapper t2e) {
    this(Optional.ofNullable(l).orElse(() -> logger).get(), null, null, t2e);
  }

  private DefaultIBDataSetJDBCIngestionFinalizerSupplier(Logger logger, PathSupplier workingPath, ConfigMapSupplier cms, TypeToExtensionMapper t2e) {
    super(logger, workingPath, cms, t2e);
  }

  @Override
  public IBDataSetFinalizerSupplier configure(ConfigMapSupplier config) {
    String q = requireNonNull(config, "Config Map Supplier").get().get(IBMetadataUtils.IBDATA_WORKING_PATH_SUPPLIER);
    getLogger().debug("" + q + " is the config working path");
    return new DefaultIBDataSetJDBCIngestionFinalizerSupplier(getLogger(), () -> Paths.get(requireNonNull(q, "Working Path Config")), config, getTypeToExtensionMapper());
  }

  @Override
  public IBDataSetFinalizer<Ingestion> get() {
    return new IngestionIBDataSetJDBCFinalizer(requireNonNull(getCms(), "Config supplier is null").get(), getWps().get());
  }

  private class IngestionIBDataSetJDBCFinalizer extends AbstractIBDataSetFinalizer<Ingestion> {

    public IngestionIBDataSetJDBCFinalizer(Map<String, String> config, Path workingPath) {
      super(config, workingPath);
    }

    @Override
    public IBChecksumPathType finalize(IBDataSet dsi2, Ingestion target, List<IBDataStreamSupplier> ibdssList) throws IOException {
      return IBDataModelUtils.forceToFinalizedPath(new Date(), getWorkingPath(), target.asDataSet(), ibdssList, getTypeToExtensionMapper());
    }

  }

}
