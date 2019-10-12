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
package org.infrastructurebuilder.data.transform.line;

import java.nio.file.Path;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.data.DefaultIBDataTransformationResult;
import org.infrastructurebuilder.data.IBDataSet;
import org.infrastructurebuilder.data.IBDataStream;
import org.infrastructurebuilder.data.IBDataStreamRecordFinalizer;
import org.infrastructurebuilder.data.IBDataTransformationResult;
import org.infrastructurebuilder.data.IBDataTransformer;
import org.infrastructurebuilder.data.IBDataTransformerSupplier;
import org.infrastructurebuilder.data.IBMetadataUtils;
import org.infrastructurebuilder.data.transform.AbstractIBDataTransformer;
import org.infrastructurebuilder.data.transform.AbstractIBDataTransformerSupplier;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;

@Named(PassThruTransformerSupplier.NAME)
public class PassThruTransformerSupplier extends AbstractIBDataTransformerSupplier {

  public static final String NAME = "pass-thru";

  @Inject
  public PassThruTransformerSupplier(@Named(IBMetadataUtils.IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps) {
    this(wps, null);
  }

  public PassThruTransformerSupplier(PathSupplier wps, ConfigMapSupplier cms) {
    super(wps, cms);
  }

  @Override
  public IBDataTransformerSupplier configure(ConfigMapSupplier cms) {
    return new PassThruTransformerSupplier(getWps(), cms);
  }

  @Override
  protected IBDataTransformer getUnconfiguredTransformerInstance(Path wps2) {
    return new PassThruTransformer(wps2);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public PassThruTransformerSupplier withFinalizer(IBDataStreamRecordFinalizer finalizer) {
    return new PassThruTransformerSupplier(getWps());
  }

  private final class PassThruTransformer extends AbstractIBDataTransformer implements IBDataTransformer {

    public PassThruTransformer(Path wps2) {
      super(wps2);
    }

    @Override
    public IBDataTransformationResult transform(IBDataSet ds, List<IBDataStream> suppliedStreams, boolean failOnError) {
      return new DefaultIBDataTransformationResult(ds);
    }

    @Override
    public String getHint() {
      return NAME;
    }
  }
}
