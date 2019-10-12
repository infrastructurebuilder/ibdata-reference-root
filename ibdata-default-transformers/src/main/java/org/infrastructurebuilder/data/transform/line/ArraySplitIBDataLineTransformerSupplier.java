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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;

import org.infrastructurebuilder.data.IBMetadataUtils;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;

@Named(ArraySplitIBDataLineTransformerSupplier.REGEX_ARRAY_SPLIT)
public class ArraySplitIBDataLineTransformerSupplier extends AbstractIBDataRecordTransformerSupplier<String, String[]> {
  public static final String REGEX_ARRAY_SPLIT = "regex-array-split";

  @javax.inject.Inject
  public ArraySplitIBDataLineTransformerSupplier(
      @Named(IBMetadataUtils.IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps) {
    this(wps, null);
  }

  private ArraySplitIBDataLineTransformerSupplier(PathSupplier wps, ConfigMapSupplier cms) {
    super(wps, cms);
  }

  @Override
  public ArraySplitIBDataLineTransformerSupplier configure(ConfigMapSupplier cms) {
    // TODO Auto-generated method stub
    return new ArraySplitIBDataLineTransformerSupplier(getWps(), cms);
  }

  @Override
  protected IBDataRecordTransformer<String, String[]> getUnconfiguredTransformerInstance(Path workingPath) {
    // TODO Auto-generated method stub
    return new ArraySplitIBDataLineTransformer(workingPath);
  }

  private class ArraySplitIBDataLineTransformer extends AbstractIBDataRecordTransformer<String, String[]> {

    public static final String REGEX = "regex";
    public static final String DEFAULT_SPLIT_REGEX = ",";
    private final String splitRegex;

    ArraySplitIBDataLineTransformer(Path workingPath) {
      this(workingPath, new HashMap<>());
    }

    /**
     * @param ps
     * @param config
     */
    ArraySplitIBDataLineTransformer(Path ps, Map<String, String> config) {
      super(ps, config);
      this.splitRegex = getConfiguration(REGEX, DEFAULT_SPLIT_REGEX);
    }

    @Override
    public IBDataRecordTransformer<String, String[]> configure(Map<String, String> cms) {
      return new ArraySplitIBDataLineTransformer(getWorkingPath(), cms);
    }

    @Override
    public String[] apply(String t) {
      return Optional.ofNullable(t).map(s -> s.split(splitRegex)).orElse(null);
    }

    @Override
    public String getHint() {
      return REGEX_ARRAY_SPLIT;
    }

  }

  @Override
  public String getHint() {
    return REGEX_ARRAY_SPLIT;
  }
}