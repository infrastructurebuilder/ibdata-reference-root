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

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.infrastructurebuilder.data.IBDataException;

@Named(DefaultMapSSToGenericRecordIBDataLineTransformer.NAME)
public class DefaultMapSSToGenericRecordIBDataLineTransformer
    extends AbstractMapSSToGenericRecordIBDataLineTransformer {

  public final static String NAME = "map-to-generic-avro";
  public final static String TIMESTAMP_FORMATTER = NAME + "timestamp.formatter";
  public final static String DEFAULT_TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME.toString();;
  public final static String TIME_FORMATTER = "time.formatter";
  public final static String DEFAULT_TIME_FORMATTER = "HH:MM";
  public final static String DATE_FORMATTER = "date.formatter";
  public final static String DEFAULT_DATE_FORMATTER = "mm-DD-yy";

  public final static String SCHEMA_PARAM = "schema"; // Required **

  public final static String LOCALE_LANGUAGE_PARAM = "locale.language";
  public final static String LOCALE_REGION_PARAM = "locale.region";
  private static final List<String> ACCEPTABLE_TYPES = Arrays.asList(Map.class.getCanonicalName());

  /**
   * @param workingPath
   * @param config
   */
  public DefaultMapSSToGenericRecordIBDataLineTransformer(Path workingPath, Map<String, String> config) {
    super(workingPath, config);
  }

  /**
   * @param workingPath
   */
  public DefaultMapSSToGenericRecordIBDataLineTransformer(Path workingPath) {
    this(workingPath, new HashMap<>());
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
    return ofPattern(getConfiguration(TIMESTAMP_FORMATTER, DEFAULT_TIMESTAMP_FORMATTER));
  }

  @Override
  boolean isBlankFieldNullInUnion() {
    return true;
  }

  @Override
  public Schema getSchema() {
    return getSchema.apply(
        ofNullable(getConfiguration(SCHEMA_PARAM)).orElseThrow(() -> new IBDataException(NO_SCHEMA_CONFIG_FOR_MAPPER)));
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
  public IBDataRecordTransformer<Map<String, String>, GenericRecord> configure(Map<String, String> cms) {
    return new DefaultMapSSToGenericRecordIBDataLineTransformer(getWorkingPath(), cms);
  }

}
