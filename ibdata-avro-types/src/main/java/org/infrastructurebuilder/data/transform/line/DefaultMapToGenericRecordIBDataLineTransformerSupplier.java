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

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.infrastructurebuilder.data.IBDataAvroUtils.NO_SCHEMA_CONFIG_FOR_MAPPER;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;
import static org.infrastructurebuilder.data.IBDataConstants.LOCALE_LANGUAGE_PARAM;
import static org.infrastructurebuilder.data.IBDataConstants.LOCALE_REGION_PARAM;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.infrastructurebuilder.data.Formatters;
import org.infrastructurebuilder.data.IBDataAvroUtils;
import org.infrastructurebuilder.data.IBDataAvroUtilsSupplier;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.slf4j.Logger;

@Named(DefaultMapToGenericRecordIBDataLineTransformerSupplier.NAME)
public class DefaultMapToGenericRecordIBDataLineTransformerSupplier
    extends AbstractIBDataRecordTransformerSupplier<Map<String, Object>, IndexedRecord> {
  public final static String NAME = "map-to-generic-avro";
  public static final List<String> ACCEPTABLE_TYPES = Arrays.asList(Map.class.getCanonicalName());
  public final static String SCHEMA_PARAM = "schema"; // Required **
  private final IBDataAvroUtilsSupplier aus;

  @Inject
  public DefaultMapToGenericRecordIBDataLineTransformerSupplier(@Named(IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps,
      LoggerSupplier l, IBDataAvroUtilsSupplier aus) {
    this(wps, null, l, aus);
  }

  private DefaultMapToGenericRecordIBDataLineTransformerSupplier(PathSupplier wps, ConfigMapSupplier cms,
      LoggerSupplier l, IBDataAvroUtilsSupplier aus) {
    super(wps, cms, l);
    this.aus = requireNonNull(aus);
  }

  @Override
  public String getHint() {
    return NAME;
  }

  @Override
  public DefaultMapToGenericRecordIBDataLineTransformerSupplier configure(ConfigMapSupplier cms) {
    return new DefaultMapToGenericRecordIBDataLineTransformerSupplier(getWps(), cms, () -> getLogger(),
        (IBDataAvroUtilsSupplier) this.aus.configure(cms.get()));
  }

  @Override
  protected IBDataRecordTransformer<Map<String, Object>, IndexedRecord> getUnconfiguredTransformerInstance(
      Path workingPath) {
    return new DefaultMapSSToGenericRecordIBDataLineTransformer(workingPath, getLogger(), aus.get());
  }

  public static class DefaultMapSSToGenericRecordIBDataLineTransformer
      extends AbstractMapToAvroGenericRecordIBDataLineTransformer {

    private final Schema schema;
    private final Formatters formatters;
    private final IBDataAvroUtils aus;

    /**
     * @param workingPath
     * @param config
     */
    private DefaultMapSSToGenericRecordIBDataLineTransformer(Path workingPath, ConfigMap config, Logger l,
        IBDataAvroUtils aus) {
      super(workingPath, config, l);
      this.aus = requireNonNull(aus);
      if (config != null && !config.keySet().contains(SCHEMA_PARAM))
        throw new IBDataException(NO_SCHEMA_CONFIG_FOR_MAPPER);
      if (config == null) {

      } else {

      }
      this.schema = config == null ? null
          : aus.avroSchemaFromString(ofNullable(getConfiguration(SCHEMA_PARAM))
              .orElseThrow(() -> new IBDataException(NO_SCHEMA_CONFIG_FOR_MAPPER + " (invalid?)")));
      this.formatters = new Formatters(Optional.ofNullable(getConfig()).orElse(new ConfigMap()));

    }

    /**
     * @param workingPath
     */
    public DefaultMapSSToGenericRecordIBDataLineTransformer(Path workingPath, Logger l, IBDataAvroUtils aus) {
      this(workingPath, null, l, aus);
    }

    @Override
    Formatters getFormatters() {
      return this.formatters;
    }

    @Override
    public Schema getSchema() {
      return this.schema;
    }

    @Override
    Locale getLocale() {
      return ofNullable(getConfiguration(LOCALE_LANGUAGE_PARAM)).map(language -> {
        Locale.Builder lb = new Locale.Builder().setLanguage(language);
        ofNullable(getConfiguration(LOCALE_REGION_PARAM)).ifPresent(region -> lb.setRegion(region));
        return lb.build();
      }).orElse(Locale.getDefault());
    }

    @Override
    public String getHint() {
      return NAME;
    }

    @Override
    public IBDataRecordTransformer<Map<String, Object>, IndexedRecord> configure(ConfigMap cms) {
      return new DefaultMapSSToGenericRecordIBDataLineTransformer(getWorkingPath(), cms, getLogger(), aus);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<Map<String, Object>> getInboundClass() {
      Class<?> clazz = Map.class;
      return (Class<Map<String, Object>>) clazz;
    }

    @Override
    public Class<IndexedRecord> getOutboundClass() {
      return IndexedRecord.class;
    }

  }

}