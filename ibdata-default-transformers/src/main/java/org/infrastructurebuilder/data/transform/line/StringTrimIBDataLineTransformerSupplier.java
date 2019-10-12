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

import static java.util.Optional.ofNullable;

import java.nio.file.Path;
import java.util.Map;

import javax.inject.Named;

import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;

@Named(StringTrimIBDataLineTransformerSupplier.STRING_TRIM)
public class StringTrimIBDataLineTransformerSupplier extends AbstractIBDataRecordTransformerSupplier<String, String> {
  public static final String STRING_TRIM = "string-trim";

  @javax.inject.Inject
  public StringTrimIBDataLineTransformerSupplier(PathSupplier wps) {
    this(wps, null);
  }

  private StringTrimIBDataLineTransformerSupplier(PathSupplier wps, ConfigMapSupplier cms) {
    super(wps, cms);
  }

  @Override
  public AbstractIBDataRecordTransformerSupplier<String, String> configure(ConfigMapSupplier cms) {
    return new StringTrimIBDataLineTransformerSupplier(getWps(), cms);
  }

  @Override
  protected IBDataRecordTransformer<String, String> getUnconfiguredTransformerInstance(Path workingPath) {
    return new StringTrimIBDataLineTransformer(workingPath);
  }

  private class StringTrimIBDataLineTransformer extends AbstractIBDataRecordTransformer<String, String> {

    /**
     * @param ps
     * @param config
     */
    protected StringTrimIBDataLineTransformer(Path ps, Map<String, String> config) {
      super(ps, config);
    }

    /**
     * @param ps
     */
    protected StringTrimIBDataLineTransformer(Path ps) {
      super(ps);
    }

    @Override
    public String apply(String t) {
      return ofNullable(t).map(s -> s.trim()).orElse(null);
    }

    @Override
    public String getHint() {
      return STRING_TRIM;
    }

    @Override
    public IBDataRecordTransformer<String, String> configure(Map<String, String> cms) {
      return new StringTrimIBDataLineTransformer(getWorkingPath(), cms);
    }
  }

  @Override
  public String getHint() {
    return STRING_TRIM;
  }
}
