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
package org.infrastructurebuilder.data.transform;

import static java.util.Objects.requireNonNull;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;
import static org.infrastructurebuilder.data.IBDataModelUtils.forceToFinalizedPath;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.sisu.Nullable;
import org.infrastructurebuilder.data.AbstractIBDataSetFinalizer;
import org.infrastructurebuilder.data.AbstractIBDataSetFinalizerSupplier;
import org.infrastructurebuilder.data.IBDataSet;
import org.infrastructurebuilder.data.IBDataSetFinalizer;
import org.infrastructurebuilder.data.IBDataSetFinalizerSupplier;
import org.infrastructurebuilder.data.IBDataStreamSupplier;
import org.infrastructurebuilder.data.model.DataSet;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("default-transform")
public class DefaultIBDataSetTransformationFinalizerSupplier extends AbstractIBDataSetFinalizerSupplier {

  public final static Logger logger = LoggerFactory.getLogger(DefaultIBDataSetTransformationFinalizerSupplier.class);

  @Inject
  public DefaultIBDataSetTransformationFinalizerSupplier(@Nullable @Named("maven-log") LoggerSupplier l,
      TypeToExtensionMapper t2e) {
    this(Optional.ofNullable(l).orElse(() -> logger).get(), null, null, t2e);
  }

  private DefaultIBDataSetTransformationFinalizerSupplier(Logger logger, PathSupplier workingPath,
      ConfigMapSupplier cms, TypeToExtensionMapper t2e) {
    super(logger, workingPath, cms, t2e);
  }

  @Override
  public IBDataSetFinalizerSupplier forceOverrideOfWorkingPath(PathSupplier wps) {
    return new DefaultIBDataSetTransformationFinalizerSupplier(getLog(), wps, getConfig(), getTypeToExtensionMapper());
  }

  @Override
  public DefaultIBDataSetTransformationFinalizerSupplier getConfiguredSupplier(ConfigMapSupplier cms) {
    return new DefaultIBDataSetTransformationFinalizerSupplier(getLog(),
        () -> Paths.get(
            requireNonNull(requireNonNull(cms).get().getString(IBDATA_WORKING_PATH_SUPPLIER), "Working Path Config")),
        cms, getTypeToExtensionMapper());
  }

  @Override
  protected IBDataSetFinalizer<Transformation> configuredType(ConfigMapSupplier config) {
    return new TransformationIBDataSetFinalizer(requireNonNull(getConfig(), "Config supplier is null").get(),
        getWps().get());
  }

  private class TransformationIBDataSetFinalizer extends AbstractIBDataSetFinalizer<Transformation> {

    public TransformationIBDataSetFinalizer(ConfigMap config, Path workingPath) {
      super(config, workingPath.resolve(UUID.randomUUID().toString()));
    }

    @Override
    public IBChecksumPathType finalize(IBDataSet inboundDataSet, Transformation target,
        List<IBDataStreamSupplier> ibdssList) throws IOException {
      DataSet targetDataSet = target.asDataSet();
      targetDataSet.setPath(inboundDataSet.getPath());

      return forceToFinalizedPath(new Date(), getWorkingPath(), targetDataSet, ibdssList, getTypeToExtensionMapper());
    }

  }

}
