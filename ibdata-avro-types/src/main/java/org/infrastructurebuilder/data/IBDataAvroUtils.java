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
package org.infrastructurebuilder.data;

import static java.util.Optional.ofNullable;
import static org.infrastructurebuilder.IBConstants.FILE_PREFIX;
import static org.infrastructurebuilder.IBConstants.HTTPS_PREFIX;
import static org.infrastructurebuilder.IBConstants.HTTP_PREFIX;
import static org.infrastructurebuilder.IBConstants.ZIP_PREFIX;
import static org.infrastructurebuilder.data.IBDataException.cet;
import static org.infrastructurebuilder.data.transform.line.DefaultMapToGenericRecordIBDataLineTransformerSupplier.SCHEMA_PARAM;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.data.TimeConversions;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.infrastructurebuilder.util.config.ConfigMap;

public interface IBDataAvroUtils {
  public static final String NO_SCHEMA_CONFIG_FOR_MAPPER = "No schema config for mapper";

  public static final Function<String, Schema> avroSchemaFromString = schema -> {
    String s = ofNullable(schema).orElseThrow(() -> new IBDataException(NO_SCHEMA_CONFIG_FOR_MAPPER + "3"));
    try {
      s = (Files.exists(Paths.get(schema))) ? Paths.get(schema).toUri().toURL().toExternalForm() : s;
    } catch (MalformedURLException e1) {
      // Do nothing.  see if it works!
    }

    boolean isURL = s.startsWith(HTTP_PREFIX) || s.startsWith(HTTPS_PREFIX) || s.startsWith(FILE_PREFIX)
        || s.startsWith(ZIP_PREFIX);
    try (InputStream in = isURL ? new URL(s).openStream() : IBDataAvroUtils.class.getResourceAsStream(s)) {
      return cet.withReturningTranslation(() -> new Schema.Parser().parse(in));
    } catch (IOException e) {
      throw new IBDataException(e); // Handles the clos() of try-with-resources
    }
  };

  public final static BiFunction<Path, ConfigMap, DataFileWriter<GenericRecord>> fromMapAndWP = (workingPath, map) -> {
    // Get the schema or die
    Optional<String> schema = ofNullable(Objects.requireNonNull(map).getString(SCHEMA_PARAM));
    Schema s = avroSchemaFromString
        .apply(schema.orElseThrow(() -> new IBDataException(NO_SCHEMA_CONFIG_FOR_MAPPER + " 2")));
    // Get the DataFileWriter or die
    DataFileWriter<GenericRecord> w = new DataFileWriter<GenericRecord>(new GenericDatumWriter<GenericRecord>(s));
    // create the working data file or die
    cet.withTranslation(() -> w.create(s, workingPath.toFile()));
    return w;
  };
  public final static BiFunction<Path, Schema, DataFileWriter<GenericRecord>> fromSchemaAndPath = (workingPath, s) -> {
    // Get the DataFileWriter or die
    DataFileWriter<GenericRecord> w = new DataFileWriter<GenericRecord>(new GenericDatumWriter<GenericRecord>(s));
    // create the working data file or die
    cet.withTranslation(() -> w.create(s, workingPath.toFile()));
    return w;
  };

  public static Object managedValue(Schema f, String k, Object strValue, Formatters formatters) {
    if (f.isUnion()) {
      return fromUnion(f, strValue, formatters);
    } else {
      return fromTypeLogicalType(f, ofNullable(f.getLogicalType()).map(LogicalType::getName), strValue, formatters);
    }
  }

  /**
   * Generate a value by figuring out the union fields
   * @param f
   * @param strValue
   * @return
   */
  public static Object fromUnion(Schema f, Object in, Formatters formatters) {
    Schema[] types = f.getTypes().toArray(new Schema[0]);

    // Nullable field as element of a 2-part union
    if ("null".equals(types[0].getName()))
      if (types.length == 2) {
        String strValue = ofNullable(in).orElse("").toString();
        String sVal = strValue.trim();
        if (sVal.trim().length() == 0)
          return formatters.isBlankFieldNullInUnion() ? null : sVal;
        else
          return fromTypeLogicalType(types[1], ofNullable(types[1].getLogicalType()).map(LogicalType::getName), sVal,
              formatters);
      } else {
        throw new IBDataException("Null unions can only have 2 fields");
      }
    return null;
  }

  public static Object fromTypeLogicalType(Schema f, Optional<String> lType, Object in, Formatters formatters) {
    String strValue = in.toString();
    Type t = f.getType();
    switch (t) {
    case STRING:
      // Includes uuid
      return strValue;
    case BOOLEAN:
      return Boolean.parseBoolean(strValue);
    case DOUBLE:
      return Double.parseDouble(strValue);
    case FLOAT:
      return Float.parseFloat(strValue);
    case LONG:
      return lType.map(lT -> {
        switch (lT) {
        case "time-millis":
          return new TimeConversions.TimeMillisConversion()
              .toLong(LocalTime.parse(strValue, formatters.getTimeFormatter()), f, f.getLogicalType());
        case "time-micros":
          return new TimeConversions.TimeMicrosConversion()
              .toLong(LocalTime.parse(strValue, formatters.getTimeFormatter()), f, f.getLogicalType());
        case "timestamp-micros":
          return new TimeConversions.TimestampMicrosConversion().toLong(Instant.parse(strValue), f, f.getLogicalType());
        case "timestamp-millis":
          return new TimeConversions.TimestampMillisConversion().toLong(Instant.parse(strValue), f, f.getLogicalType());
        default:
          throw new IBDataException();
        }
      }).orElse(Long.parseLong(strValue));
    case INT:
      return lType.map(lT -> {
        switch (lT) {
        case "date":
          DateTimeFormatter dtf = formatters.getDateFormatter();
          return new TimeConversions.DateConversion().toInt(LocalDate.parse(strValue, dtf), f, f.getLogicalType());
        default:
          throw new IBDataException();
        }
      }).orElseGet(() -> Integer.parseInt(strValue));
    case NULL:
      return null;
    case BYTES:
      return lType.map(lT -> {
        switch (lT) {
        case "decimal":
          throw new IBDataException("Decimal not yet implemented");
        default:
          return strValue.getBytes();
        }
      });
    case FIXED:
      return lType.map(lT -> {
        switch (lT) {
        case "decimal":
          throw new IBDataException("Decimal not yet implemented");
        default:
          return Arrays.copyOf(strValue.getBytes(), f.getFixedSize());
        }
      });
    case ENUM:
      if (f.getEnumSymbols().contains(strValue))
        return strValue;
      else
        throw new IBDataException("Enum value " + strValue + " is not valid " + f.getEnumSymbols());
    case MAP:
      throw new IBDataException("Cannot build a Map from within the MSS parser : " + Type.MAP.toString());
    case ARRAY:
      // List of some type
      throw new IBDataException("Cannot build an array from within the MSS parser : " + Type.ARRAY.toString());
    case RECORD:
      // Whole new record to parse
      throw new IBDataException("Cannot build a record from within the MSS parser : " + Type.RECORD.toString());
    default:
      // UNION is all that's left
      throw new IBDataException("Should never reach this: " + Type.UNION.toString());
    }
  }

}
