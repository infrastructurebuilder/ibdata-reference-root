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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBDataSet;
import org.infrastructurebuilder.data.IBDataStream;
import org.infrastructurebuilder.data.IBDataTransformationResult;
import org.infrastructurebuilder.data.IBDataTransformer;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;

@Named(StreamJoiningTransformSupplier.STREAM_JOIN)
public class StreamJoiningTransformSupplier extends AbstractIBDataTransformerSupplier {

  public static final String STREAM_JOIN = "stream-join";

  @Inject
  public StreamJoiningTransformSupplier(IBRuntimeUtils ibr) {
    this(ibr, null);
  }

  private StreamJoiningTransformSupplier(IBRuntimeUtils ibr, ConfigMapSupplier cms) {
    super(ibr, cms);
  }

  @Override
  public StreamJoiningTransformSupplier configure(ConfigMapSupplier cms) {
    return new StreamJoiningTransformSupplier(getRuntimeUtils(), cms);
  }

  @Override
  protected IBDataTransformer getConfiguredTransformerInstance() {
    return new StreamJoiningTransformer(getRuntimeUtils());
  }

  private class StreamJoiningTransformer extends AbstractIBDataTransformer {

    public StreamJoiningTransformer(IBRuntimeUtils ibr) {
      this(ibr, new ConfigMap());
    }

    public StreamJoiningTransformer(IBRuntimeUtils ibr, ConfigMap config) {
      super(ibr, config);
    }

    @Override
    public String getHint() {
      return STREAM_JOIN;
    }

    @Override
    public IBDataTransformationResult transform(Transformer transformer, IBDataSet ds,
        List<IBDataStream> suppliedStreams, boolean failOnError) {
      throw new IBDataException("Stream Joiner Not implemented ");
      // return new DefaultIBDataTransformationResult(null);
    }

  }

}
