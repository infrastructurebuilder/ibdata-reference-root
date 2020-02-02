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
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;
import org.slf4j.Logger;

@Named(PassThruTransformerSupplier.NAME)
public class PassThruTransformerSupplier extends AbstractIBDataTransformerSupplier {

  public static final String NAME = "pass-thru";

  @Inject
  public PassThruTransformerSupplier(IBRuntimeUtils ibr) {
    this(ibr, null);
  }

  public PassThruTransformerSupplier(IBRuntimeUtils ibr, ConfigMapSupplier cms) {
    super(ibr, cms);
  }

  @Override
  public PassThruTransformerSupplier configure(ConfigMapSupplier cms) {
    return new PassThruTransformerSupplier(getRuntimeUtils(), cms);
  }

  @Override
  protected IBDataTransformer getConfiguredTransformerInstance() {
    return new PassThruTransformer(getRuntimeUtils());
  }

  @Override
  public PassThruTransformerSupplier withFinalizer(IBDataStreamRecordFinalizer<?> finalizer) {
    return new PassThruTransformerSupplier(getRuntimeUtils());
  }

  private final class PassThruTransformer extends AbstractIBDataTransformer {

    public PassThruTransformer(IBRuntimeUtils ibr) {
      super(ibr);
    }

    @Override
    public IBDataTransformationResult transform(Transformer transformer, IBDataSet ds,
        List<IBDataStream> suppliedStreams, boolean failOnError) {
      return new DefaultIBDataTransformationResult(ds, getWorkingPath());
    }

    @Override
    public String getHint() {
      return NAME;
    }
  }
}
