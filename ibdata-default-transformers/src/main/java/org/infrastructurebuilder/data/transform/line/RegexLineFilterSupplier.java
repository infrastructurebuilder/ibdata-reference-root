
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
import java.util.Optional;
import java.util.regex.Pattern;

import javax.inject.Named;

import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;

@Named(RegexLineFilterSupplier.REGEX_LINE_FILTER)
public class RegexLineFilterSupplier extends AbstractIBDataRecordTransformerSupplier<String, String> {
  public static final String REGEX_LINE_FILTER = "regex-line-filter";

  @javax.inject.Inject
  public RegexLineFilterSupplier(
      @Named(IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps) {
    this(wps, null);
  }

  private RegexLineFilterSupplier(PathSupplier wps, ConfigMapSupplier cms) {
    super(wps, cms);
  }

  @Override
  public RegexLineFilterSupplier configure(ConfigMapSupplier cms) {
    return new RegexLineFilterSupplier(getWps(), cms);
  }

  @Override
  protected IBDataRecordTransformer<String, String> getUnconfiguredTransformerInstance(Path workingPath) {
    return new RegexLineFilter(workingPath);
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
    public RegexLineFilter(Path ps, ConfigMap config) {
      super(ps, config);
      this.splitRegex = Pattern.compile(Pattern.quote(getConfiguration(REGEX, DEFAULT_REGEX)));
    }

    /**
     * @param ps
     */
    public RegexLineFilter(Path ps) {
      this(ps, new ConfigMap());
    }

    @Override
    public IBDataRecordTransformer<String, String> configure(ConfigMap cms) {
      return new RegexLineFilter(getWorkingPath(), cms);
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
    public Optional<List<String>> accepts() {
      return Optional.of(ACCEPTABLE_TYPES);
    }

    @Override
    public Optional<String> produces() {
      return Optional.of(String.class.getCanonicalName());
    }

  }

  @Override
  public String getHint() {
    return REGEX_LINE_FILTER;
  }

}