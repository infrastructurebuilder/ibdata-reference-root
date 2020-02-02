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
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;
import org.slf4j.Logger;

@Named(ArrayJoinIBDataLineTransformerSupplier.TOSTRING_ARRAY_JOIN)
public class ArrayJoinIBDataLineTransformerSupplier extends AbstractIBDataRecordTransformerSupplier<Object[], String> {
  public static final String TOSTRING_ARRAY_JOIN = "tostring-array-join";
  public static final List<String> ACCEPTABLE_TYPES = Arrays.asList(Array.class.getCanonicalName());

  @javax.inject.Inject
  public ArrayJoinIBDataLineTransformerSupplier(IBRuntimeUtils ibr) {
    this(ibr, null);
  }

  private ArrayJoinIBDataLineTransformerSupplier(IBRuntimeUtils wps, ConfigMapSupplier cms) {
    super(wps, cms);
  }

  @Override
  public ArrayJoinIBDataLineTransformerSupplier configure(ConfigMapSupplier cms) {
    return new ArrayJoinIBDataLineTransformerSupplier(getRuntimeUtils(), cms);
  }

  @Override
  protected IBDataRecordTransformer<Object[], String> getUnconfiguredTransformerInstance() {
    return new ArrayJoinIBDataLineTransformer(getRuntimeUtils());
  }

  @Override
  public String getHint() {
    return TOSTRING_ARRAY_JOIN;
  }

  private class ArrayJoinIBDataLineTransformer extends AbstractIBDataRecordTransformer<Object[], String> {

    public static final String DELIMITER = "delimiter";
    public static final String PREFIX = "prefix";
    public static final String SUFFIX = "suffix";
    public static final String DEFAULT_DELIMITER = ",";
    private final String delimiter;
    private final Optional<String> prefix, suffix;

    ArrayJoinIBDataLineTransformer(IBRuntimeUtils workingPath) {
      this(workingPath, new ConfigMap());
    }

    /**
     * @param ps
     * @param config
     */
    ArrayJoinIBDataLineTransformer(IBRuntimeUtils ps, ConfigMap config) {
      super(ps, config);
      this.delimiter = Pattern.quote(getConfiguration(DELIMITER, DEFAULT_DELIMITER));
      this.prefix = Optional.ofNullable(getConfiguration(PREFIX));
      this.suffix = Optional.ofNullable(getConfiguration(SUFFIX));
    }

    @Override
    public IBDataRecordTransformer<Object[], String> configure(ConfigMap cms) {
      return new ArrayJoinIBDataLineTransformer(getRuntimeUtils(), cms);
    }

    @Override
    public String apply(Object[] t) {
      if (t == null)
        return null;
      Collector<CharSequence, ?, String> joiner = this.prefix.isPresent()
          ? Collectors.joining(this.delimiter, this.prefix.get(), this.suffix.orElse(this.prefix.get()))
          : Collectors.joining(this.delimiter);
      return Arrays.asList(t).stream().map(o -> o.toString()).collect(joiner);
    }

    @Override
    public String getHint() {
      return TOSTRING_ARRAY_JOIN;
    }

    @Override
    public Class<Object[]> getInboundClass() {
      return Object[].class;
    }

    @Override
    public Class<String> getOutboundClass() {
      return String.class;
    }

  }

}