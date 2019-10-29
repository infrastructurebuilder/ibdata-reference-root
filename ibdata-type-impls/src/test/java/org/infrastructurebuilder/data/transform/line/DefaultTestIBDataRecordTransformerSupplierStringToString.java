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

import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;

public class DefaultTestIBDataRecordTransformerSupplierStringToString
    extends AbstractIBDataRecordTransformerSupplier<String, String> {

  protected DefaultTestIBDataRecordTransformerSupplierStringToString(PathSupplier wps, ConfigMapSupplier cms) {
    super(wps, cms);
  }

  @Override
  public String getHint() {
    return DefaultTestIBDataRecordTransformerSupplierStringToString.class.getCanonicalName();
  }

  @Override
  public AbstractIBDataRecordTransformerSupplier<String, String> configure(ConfigMapSupplier cms) {
    return this;
  }

  @Override
  protected IBDataRecordTransformer<String, String> getUnconfiguredTransformerInstance(Path workingPath) {
    return new StringToStringRecordTransformer(getWps().get());
  }

  public static class StringToStringRecordTransformer extends AbstractIBDataRecordTransformer<String, String> {

    public StringToStringRecordTransformer(Path path) {
      super(path);
    }

    private StringToStringRecordTransformer(Path workingPath, ConfigMap config) {
      super(workingPath, config);
    }

    @Override
    public String getHint() {
      return StringToStringRecordTransformer.class.getCanonicalName();
    }

    @Override
    public IBDataRecordTransformer<String,String> configure(ConfigMap config) {
      return new StringToStringRecordTransformer(getWorkingPath(), config);
    }

    @Override
    public String apply(String t) {
      return t;
    }

  }

}
