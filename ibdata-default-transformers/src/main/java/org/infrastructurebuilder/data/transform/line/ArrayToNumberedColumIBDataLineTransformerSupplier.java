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

import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;

import org.infrastructurebuilder.data.IBMetadataUtils;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;

@Named(ArrayToNumberedColumIBDataLineTransformerSupplier.ARRAY_TO_NUMBERED_COL)
public class ArrayToNumberedColumIBDataLineTransformerSupplier
    extends AbstractIBDataRecordTransformerSupplier<String[], Map<String, String>> {
  public static final String ARRAY_TO_NUMBERED_COL = "array-to-numbered-column";

  @javax.inject.Inject
  public ArrayToNumberedColumIBDataLineTransformerSupplier(
      @Named(IBMetadataUtils.IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps) {
    this(wps, null);
  }

  private ArrayToNumberedColumIBDataLineTransformerSupplier(PathSupplier wps, ConfigMapSupplier cms) {
    super(wps, cms);
  }

  @Override
  public ArrayToNumberedColumIBDataLineTransformerSupplier configure(ConfigMapSupplier cms) {
    return new ArrayToNumberedColumIBDataLineTransformerSupplier(getWps(), cms);
  }

  @Override
  protected IBDataRecordTransformer<String[], Map<String, String>> getUnconfiguredTransformerInstance(
      Path workingPath) {
    return new ArrayToNumberedColumIBDataLineTransformer(workingPath);
  }

  private class ArrayToNumberedColumIBDataLineTransformer
      extends AbstractIBDataRecordTransformer<String[], Map<String, String>> {

    public static final String FORMAT_KEY = "format";
    public final static String FORMAT = "COLUMN%00d";
    private final List<String> ACCEPTABLE_TYPES = Arrays.asList(Array.class.getCanonicalName());
    private final String format;

    /**
     * @param ps
     * @param config
     */
    protected ArrayToNumberedColumIBDataLineTransformer(Path ps, ConfigMap config) {
      super(ps, config);
      this.format = getConfiguration(FORMAT_KEY, FORMAT);
    }

    /**
     * @param ps
     */
    protected ArrayToNumberedColumIBDataLineTransformer(Path ps) {
      super(ps);
      this.format = null;
    }

    @Override
    public Map<String, String> apply(String[] a) {
      Map<String, String> m = new HashMap<>();
      for (int i = 0; i < a.length; ++i) {
        m.put(String.format(format, i), a[i]);
      }
      return m;
    }

    @Override
    public String getHint() {
      return ARRAY_TO_NUMBERED_COL;
    }

    @Override
    public IBDataRecordTransformer<String[], Map<String, String>> configure(ConfigMap cms) {
      return new ArrayToNumberedColumIBDataLineTransformer(getWorkingPath(), cms);
    }

    @Override
    public Optional<List<String>> accepts() {
      return Optional.of(ACCEPTABLE_TYPES);
    }
    @Override
    public Optional<String> produces() {
      return Optional.of(Map.class.getCanonicalName());
    }
  }

  @Override
  public String getHint() {
    return ARRAY_TO_NUMBERED_COL;
  }


}
