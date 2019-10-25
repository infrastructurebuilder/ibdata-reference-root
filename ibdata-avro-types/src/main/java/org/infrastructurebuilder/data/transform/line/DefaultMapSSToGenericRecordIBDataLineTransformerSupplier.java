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

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Optional.ofNullable;
import static org.infrastructurebuilder.data.IBDataAvroUtils.NO_SCHEMA_CONFIG_FOR_MAPPER;
import static org.infrastructurebuilder.data.IBDataAvroUtils.getSchema;
import static org.infrastructurebuilder.data.transform.line.DefaultMapSSToGenericRecordIBDataLineTransformerSupplier.SCHEMA_PARAM;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBMetadataUtils;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;

@Named(DefaultMapSSToGenericRecordIBDataLineTransformerSupplier.NAME)
public class DefaultMapSSToGenericRecordIBDataLineTransformerSupplier
    extends AbstractIBDataRecordTransformerSupplier<Map<String, String>, GenericRecord> {
  public final static String NAME = "map-to-generic-avro";
  public static final List<String> ACCEPTABLE_TYPES = Arrays.asList(Map.class.getCanonicalName());
  public final static DateTimeFormatter DEFAULT_TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;
  public final static String SCHEMA_PARAM = "schema"; // Required **
  public final static String TIMESTAMP_FORMATTER = "timestamp.formatter";
  public final static String TIME_FORMATTER = "time.formatter";
  public final static String DEFAULT_TIME_FORMATTER = "HH:MM";
  public final static String DATE_FORMATTER = "date.formatter";
  public final static String DEFAULT_DATE_FORMATTER = "mm-DD-yy";

  public final static String LOCALE_LANGUAGE_PARAM = "locale.language";
  public final static String LOCALE_REGION_PARAM = "locale.region";

  @Inject
  public DefaultMapSSToGenericRecordIBDataLineTransformerSupplier(
      @Named(IBMetadataUtils.IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps) {
    this(wps, null);
  }

  private DefaultMapSSToGenericRecordIBDataLineTransformerSupplier(PathSupplier wps, ConfigMapSupplier cms) {
    super(wps, cms);
  }

  @Override
  public String getHint() {
    return NAME;
  }

  @Override
  public DefaultMapSSToGenericRecordIBDataLineTransformerSupplier configure(ConfigMapSupplier cms) {
    return new DefaultMapSSToGenericRecordIBDataLineTransformerSupplier(getWps(), cms);
  }

  @Override
  protected IBDataRecordTransformer<Map<String, String>, GenericRecord> getUnconfiguredTransformerInstance(
      Path workingPath) {
    return new DefaultMapSSToGenericRecordIBDataLineTransformer(workingPath);
  }

  public static class DefaultMapSSToGenericRecordIBDataLineTransformer
      extends AbstractMapSSToGenericRecordIBDataLineTransformer {

    private final Schema schema;

    /**
     * @param workingPath
     * @param config
     */
    public DefaultMapSSToGenericRecordIBDataLineTransformer(Path workingPath, ConfigMap config) {
      super(workingPath, config);

      if (config != null && !config.keySet().contains(SCHEMA_PARAM))
        throw new IBDataException(NO_SCHEMA_CONFIG_FOR_MAPPER);
      this.schema = config == null ? null
          : getSchema.apply(ofNullable(getConfiguration(SCHEMA_PARAM))
              .orElseThrow(() -> new IBDataException(NO_SCHEMA_CONFIG_FOR_MAPPER + " (invalid?)")));
    }

    /**
     * @param workingPath
     */
    public DefaultMapSSToGenericRecordIBDataLineTransformer(Path workingPath) {
      this(workingPath, null);
    }

    @Override
    DateTimeFormatter getDateFormatter() {
      return ofPattern(getConfiguration(DATE_FORMATTER, DEFAULT_DATE_FORMATTER));
    }

    @Override
    DateTimeFormatter getTimeFormatter() {
      return ofPattern(getConfiguration(TIME_FORMATTER, DEFAULT_TIME_FORMATTER));
    }

    @Override
    DateTimeFormatter getTimestampFormatter() {
      return ofNullable(getObjectConfiguration(TIMESTAMP_FORMATTER, null)).map(Object::toString)
          .map(DateTimeFormatter::ofPattern).orElse(DEFAULT_TIMESTAMP_FORMATTER);
    }

    @Override
    boolean isBlankFieldNullInUnion() {
      return true;
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
    public Optional<List<String>> accepts() {
      return Optional.of(ACCEPTABLE_TYPES);
    }

    @Override
    public Optional<String> produces() {
      return Optional.of(GenericRecord.class.getCanonicalName());
    }

    @Override
    public IBDataRecordTransformer<Map<String, String>, GenericRecord> configure(ConfigMap cms) {
      return new DefaultMapSSToGenericRecordIBDataLineTransformer(getWorkingPath(), cms);
    }

  }

}