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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;
import org.slf4j.Logger;

@Named(ArrayToNameMapIBDataLineTransformerSupplier.ARRAY_TO_NAME_MAP)
public class ArrayToNameMapIBDataLineTransformerSupplier
    extends AbstractIBDataRecordTransformerSupplier<String[], Map<String, String>> {
  public static final String ARRAY_TO_NAME_MAP = "array-to-name-map";

  @javax.inject.Inject
  public ArrayToNameMapIBDataLineTransformerSupplier(IBRuntimeUtils wps) {
    this(wps, null);
  }

  private ArrayToNameMapIBDataLineTransformerSupplier(IBRuntimeUtils wps, ConfigMapSupplier cms) {
    super(wps, cms);
  }

  @Override
  public ArrayToNameMapIBDataLineTransformerSupplier configure(ConfigMapSupplier cms) {
    return new ArrayToNameMapIBDataLineTransformerSupplier(getRuntimeUtils(), cms);
  }

  @Override
  protected IBDataRecordTransformer<String[], Map<String, String>> getUnconfiguredTransformerInstance() {
    return new ArrayToNameMapIBDataLineTransformer(getRuntimeUtils());
  }

  @Override
  public String getHint() {
    return ARRAY_TO_NAME_MAP;
  }

  private class ArrayToNameMapIBDataLineTransformer
      extends AbstractIBDataRecordTransformer<String[], Map<String, String>> {

    private List<String> format;

    /**
     * @param ps
     * @param config
     */
    @SuppressWarnings("unchecked")
    protected ArrayToNameMapIBDataLineTransformer(IBRuntimeUtils ps, ConfigMap config) {
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
    protected ArrayToNameMapIBDataLineTransformer(IBRuntimeUtils ps) {
      super(ps);
      this.format = null;
    }

    @Override
    public Map<String, String> apply(String[] a) {
      if (format.size() > a.length)
        throw new IBDataException("Invalid length " + format.size() + " of " + format + " is creater than length "
            + a.length + " of " + Arrays.asList(a));
      Map<String, String> m = new HashMap<>();
      for (int i = 0; i < format.size(); ++i) {
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
      return new ArrayToNameMapIBDataLineTransformer(getRuntimeUtils(), cms);
    }

    @Override
    public Class<String[]> getInboundClass() {
      return String[].class;
    }

    @Override
    public Class<Map<String, String>> getOutboundClass() {
      Map<String, String> c = new HashMap<>();
      return (Class<Map<String, String>>) c.getClass();
    }

  }

}
