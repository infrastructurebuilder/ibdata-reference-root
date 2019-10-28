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

import static org.infrastructurebuilder.data.transform.Record.FIELD_KEY;

import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;

import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBMetadataUtils;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;

@Named(ArrayToNameMapIBDataLineTransformerSupplier.ARRAY_TO_NAME_MAP)
public class ArrayToNameMapIBDataLineTransformerSupplier
    extends AbstractIBDataRecordTransformerSupplier<String[], Map<String, String>> {
  public static final String ARRAY_TO_NAME_MAP = "array-to-name-map";

  @javax.inject.Inject
  public ArrayToNameMapIBDataLineTransformerSupplier(
      @Named(IBMetadataUtils.IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps) {
    this(wps, null);
  }

  private ArrayToNameMapIBDataLineTransformerSupplier(PathSupplier wps, ConfigMapSupplier cms) {
    super(wps, cms);
  }

  @Override
  public ArrayToNameMapIBDataLineTransformerSupplier configure(ConfigMapSupplier cms) {
    return new ArrayToNameMapIBDataLineTransformerSupplier(getWps(), cms);
  }

  @Override
  protected IBDataRecordTransformer<String[], Map<String, String>> getUnconfiguredTransformerInstance(
      Path workingPath) {
    return new ArrayToNameMapIBDataLineTransformer(workingPath);
  }

  private class ArrayToNameMapIBDataLineTransformer
      extends AbstractIBDataRecordTransformer<String[], Map<String, String>> {

    private final List<String> ACCEPTABLE_TYPES = Arrays.asList(Array.class.getCanonicalName());
    private List<String> format;

    /**
     * @param ps
     * @param config
     */
    @SuppressWarnings("unchecked")
    protected ArrayToNameMapIBDataLineTransformer(Path ps, ConfigMap config) {
      super(ps, config);
      try {
        this.format = (List<String>) getOptionalObjectConfiguration(FIELD_KEY)
            .orElseThrow(() -> new IBDataException("No " + FIELD_KEY + " found in config"));
      } catch (ClassCastException e) {
        throw new IBDataException("Type of " + FIELD_KEY + " was not List<String>", e);
      }
    }

    /**
     * @param ps
     */
    protected ArrayToNameMapIBDataLineTransformer(Path ps) {
      super(ps);
      this.format = null;
    }

    @Override
    public Map<String, String> apply(String[] a) {
      Map<String, String> m = new HashMap<>();
      for (int i = 0; i < a.length; ++i) {
        m.put(format.get(i), a[i]);
      }
      return m;
    }

    @Override
    public String getHint() {
      return ARRAY_TO_NAME_MAP;
    }

    @Override
    public IBDataRecordTransformer<String[], Map<String, String>> configure(ConfigMap cms) {
      return new ArrayToNameMapIBDataLineTransformer(getWorkingPath(), cms);
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
    return ARRAY_TO_NAME_MAP;
  }

}
