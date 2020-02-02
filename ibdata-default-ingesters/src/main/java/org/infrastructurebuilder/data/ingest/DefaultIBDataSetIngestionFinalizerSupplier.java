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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.sisu.Typed;
import org.infrastructurebuilder.data.AbstractIBDataSetFinalizer;
import org.infrastructurebuilder.data.AbstractIBDataSetFinalizerSupplier;
import org.infrastructurebuilder.data.IBDataSet;
import org.infrastructurebuilder.data.IBDataSetFinalizerSupplier;
import org.infrastructurebuilder.data.IBDataStreamSupplier;
import org.infrastructurebuilder.data.IBSchemaDAOSupplier;
import org.infrastructurebuilder.data.model.DataSet;
import org.infrastructurebuilder.data.model.IBDataModelUtils;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.IBResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named(DefaultIBDataSetIngestionFinalizerSupplier.DEFAULT_INGEST)
@Typed(IBDataSetFinalizerSupplier.class)
public class DefaultIBDataSetIngestionFinalizerSupplier
    extends AbstractIBDataSetFinalizerSupplier<IBIngestion, Object> {

  public static final String DEFAULT_INGEST = "default-ingest";
  public final static Logger logger = LoggerFactory.getLogger(DefaultIBDataSetIngestionFinalizerSupplier.class);

  @Inject
  public DefaultIBDataSetIngestionFinalizerSupplier(IBRuntimeUtils ibr) {
    this(ibr, null);
  }

  private DefaultIBDataSetIngestionFinalizerSupplier(IBRuntimeUtils ibr, ConfigMapSupplier cms) {
    super(ibr, cms);
  }

  @Override
  public DefaultIBDataSetIngestionFinalizerSupplier getConfiguredSupplier(ConfigMapSupplier cms) {
    return new DefaultIBDataSetIngestionFinalizerSupplier(getRuntimeUtils(), cms);
  }

  @Override
  protected IngestionIBDataSetFinalizer getInstance(IBRuntimeUtils ibr, Object in) {
    return new IngestionIBDataSetFinalizer(requireNonNull(getConfig(), "Config supplier is null").get(),
        ibr.getWorkingPath());
  }

  private class IngestionIBDataSetFinalizer extends AbstractIBDataSetFinalizer<IBIngestion, Object> {

    public IngestionIBDataSetFinalizer(ConfigMap config, Path workingPath) {
      super(config, workingPath);
    }

    @Override
    public IBResource finalize(IBDataSet dsi2, IBIngestion target, List<IBDataStreamSupplier> ibdssList,
        List<IBSchemaDAOSupplier> schemaList, Optional<String> basedir) throws IOException {
      DataSet d = target.asDataSet();
      Map<String, IBDataSchemaIngestionConfig> p = target.asSchemaIngestion();
      // dsi2 is always null. There is no "previous dataset" in ingestion
      return IBDataModelUtils.forceToFinalizedPath(new Date(), getWorkingPath(), d, ibdssList, schemaList,
          getTypeToExtensionMapper(), basedir);
    }

  }

}
