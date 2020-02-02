
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
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.inject.Named;

import org.infrastructurebuilder.data.IBDataConstants;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;
import org.slf4j.Logger;

@Named(RandomlyPickLineFilterSupplier.RANDMOM_LINE_FILTER)
public class RandomlyPickLineFilterSupplier extends AbstractIBDataRecordTransformerSupplier<Object, Object> {
  public static final String RANDMOM_LINE_FILTER = "random-line-filter";

  @javax.inject.Inject
  public RandomlyPickLineFilterSupplier(IBRuntimeUtils ibr) {
    this(ibr, null);
  }

  private RandomlyPickLineFilterSupplier(IBRuntimeUtils ibr, ConfigMapSupplier cms) {
    super(ibr, cms);
  }

  @Override
  public RandomlyPickLineFilterSupplier configure(ConfigMapSupplier cms) {
    return new RandomlyPickLineFilterSupplier(getRuntimeUtils(), cms);
  }

  @Override
  protected IBDataRecordTransformer<Object, Object> getUnconfiguredTransformerInstance() {
    return new RandomLineFilter(getRuntimeUtils());
  }

  @Override
  public String getHint() {
    return RANDMOM_LINE_FILTER;
  }

  private class RandomLineFilter extends AbstractIBDataRecordTransformer<Object, Object> {

    public static final String RANDOMVAL = "percentage";
    public static final String DEFAULT_RANDOM = ".5"; // Match pretty much anything
    private final List<String> ACCEPTABLE_TYPES = Arrays.asList(IBDataConstants.ANY_TYPE);
    private final float random;
    private final Random randomGen = new Random(Instant.now().toEpochMilli());

    /**
     * @param ps
     * @param config
     */
    public RandomLineFilter(IBRuntimeUtils ps, ConfigMap config) {
      super(ps, config);
      this.random = Float.parseFloat((getConfiguration(RANDOMVAL, DEFAULT_RANDOM)));
    }

    /**
     * @param ps
     */
    public RandomLineFilter(IBRuntimeUtils ps) {
      this(ps, new ConfigMap());
    }

    @Override
    public IBDataRecordTransformer<Object, Object> configure(ConfigMap cms) {
      return new RandomLineFilter(getRuntimeUtils(), cms);
    }

    @Override
    public Object apply(Object t) {

      return ofNullable(t).flatMap(s -> ofNullable(randomGen.nextFloat() < random ? s : null)).orElse(null);
    }

    @Override
    public String getHint() {
      return RANDMOM_LINE_FILTER;
    }

    @Override
    public Class<Object> getInboundClass() {
      return Object.class;
    }

    @Override
    public Class<Object> getOutboundClass() {
      return Object.class;
    }

  }

}