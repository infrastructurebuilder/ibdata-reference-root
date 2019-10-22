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

import org.eclipse.sisu.Nullable;
import org.infrastructurebuilder.data.AbstractIBDataSetFinalizer;
import org.infrastructurebuilder.data.AbstractIBDataSetFinalizerSupplier;
import org.infrastructurebuilder.data.IBDataModelUtils;
import org.infrastructurebuilder.data.IBDataSet;
import org.infrastructurebuilder.data.IBDataSetFinalizer;
import org.infrastructurebuilder.data.IBDataSetFinalizerSupplier;
import org.infrastructurebuilder.data.IBDataStreamSupplier;
import org.infrastructurebuilder.data.IBMetadataUtils;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("default-ingest")
public class DefaultIBDataSetIngestionFinalizerSupplier extends AbstractIBDataSetFinalizerSupplier {

  public final static Logger logger = LoggerFactory.getLogger(DefaultIBDataSetIngestionFinalizerSupplier.class);

  @Inject
  public DefaultIBDataSetIngestionFinalizerSupplier(@Nullable @Named("maven-log") LoggerSupplier l,
      TypeToExtensionMapper t2e) {
    this(Optional.ofNullable(l).orElse(() -> logger).get(), null, null, t2e);
  }

  private DefaultIBDataSetIngestionFinalizerSupplier(Logger logger, PathSupplier workingPath, ConfigMapSupplier cms,
      TypeToExtensionMapper t2e) {
    super(logger, workingPath, cms, t2e);
  }

  @Override
  public IBDataSetFinalizerSupplier configure(ConfigMapSupplier config) {
    return new DefaultIBDataSetIngestionFinalizerSupplier(getLogger(), () -> Paths.get(
        //
        requireNonNull(
            // Get a config
            requireNonNull(config, "Config Map Supplier").get()
                // Get the working path because the one we have is bad? FIXME
                .get(IBMetadataUtils.IBDATA_WORKING_PATH_SUPPLIER),
            // Or report what's wrong
            "Working Path Config")),
        config, getTypeToExtensionMapper());
  }

  @Override
  public IBDataSetFinalizer<Ingestion> get() {
    return new IngestionIBDataSetFinalizer(requireNonNull(getCms(), "Config supplier is null").get(), getWps().get());
  }

  private class IngestionIBDataSetFinalizer extends AbstractIBDataSetFinalizer<Ingestion> {

    public IngestionIBDataSetFinalizer(ConfigMap config, Path workingPath) {
      super(config, workingPath);
    }

    @Override
    public IBChecksumPathType finalize(IBDataSet dsi2, Ingestion target, List<IBDataStreamSupplier> ibdssList)
        throws IOException {
      return IBDataModelUtils.forceToFinalizedPath(new Date(), getWorkingPath(), target.asDataSet(), ibdssList,
          getTypeToExtensionMapper());
    }

  }

}
