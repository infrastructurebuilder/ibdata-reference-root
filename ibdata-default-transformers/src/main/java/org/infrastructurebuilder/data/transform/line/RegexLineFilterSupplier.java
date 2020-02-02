
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
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Named;

import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.slf4j.Logger;

@Named(RegexLineFilterSupplier.REGEX_LINE_FILTER)
public class RegexLineFilterSupplier extends AbstractIBDataRecordTransformerSupplier<String, String> {
  public static final String REGEX_LINE_FILTER = "regex-line-filter";

  @javax.inject.Inject
  public RegexLineFilterSupplier(IBRuntimeUtils ibr) {
    this(ibr, null);
  }

  private RegexLineFilterSupplier(IBRuntimeUtils wps, ConfigMapSupplier cms) {
    super(wps, cms);
  }

  @Override
  public RegexLineFilterSupplier configure(ConfigMapSupplier cms) {
    return new RegexLineFilterSupplier(getRuntimeUtils(), cms);
  }

  @Override
  protected IBDataRecordTransformer<String, String> getUnconfiguredTransformerInstance() {
    return new RegexLineFilter(getRuntimeUtils(), null);
  }

  private class RegexLineFilter extends AbstractIBDataRecordTransformer<String, String> {

    public static final String REGEX = "regex";
    public static final String DEFAULT_REGEX = ".*"; // Match pretty much anything
    private final List<String> ACCEPTABLE_TYPES = Arrays.asList(String.class.getCanonicalName());
    private final Pattern splitRegex;

    /**
     * @param ps
     * @param config
     */
    public RegexLineFilter(IBRuntimeUtils ps, ConfigMap config) {
      super(ps, config);
      this.splitRegex = Pattern.compile(Pattern.quote(getConfiguration(REGEX, DEFAULT_REGEX)));
    }

    @Override
    public IBDataRecordTransformer<String, String> configure(ConfigMap cms) {
      return new RegexLineFilter(getRuntimeUtils(), cms);
    }

    @Override
    public String apply(String t) {
      return ofNullable(t).flatMap(s -> ofNullable(splitRegex.matcher(s).matches() ? s : null)).orElse(null);
    }

    @Override
    public String getHint() {
      return REGEX_LINE_FILTER;
    }

    @Override
    public Class<String> getInboundClass() {
      return String.class;
    }

    @Override
    public Class<String> getOutboundClass() {
      return String.class;
    }
  }

  @Override
  public String getHint() {
    return REGEX_LINE_FILTER;
  }

}