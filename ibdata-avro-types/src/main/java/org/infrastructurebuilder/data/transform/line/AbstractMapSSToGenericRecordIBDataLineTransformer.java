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

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.data.TimeConversions;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.slf4j.Logger;

abstract public class AbstractMapSSToGenericRecordIBDataLineTransformer
    extends AbstractGenericIBDataLineTransformer<Map<String, Object>> {

  protected AbstractMapSSToGenericRecordIBDataLineTransformer(Path workingPath, ConfigMap config, Logger l) {
    super(workingPath, config, l);
  }

  @Override
  public GenericRecord apply(Map<String, Object> t) {
    Objects.requireNonNull(t);
    Schema s = getSchema();

    final GenericRecord r = new GenericData.Record(s);
    t.keySet().forEach(k -> {
      Field v = s.getField(k);
      if (v != null) {
        r.put(k, managedValue(v.schema(), k, t.get(k)));
      } else {
        // FIXME (I need a logger to warn of missing fields)
      }
    }); // FIXME mebbe we need to catch some of the RuntimeException instances
    return r;
  }

  private Object managedValue(Schema f, String k, Object strValue) {
    if (f.isUnion()) {
      return valueFromUnion(f, strValue);
    } else {
      return valueFromTypeLogicalType(f, Optional.ofNullable(f.getLogicalType()).map(LogicalType::getName), strValue);
    }
  }

  abstract DateTimeFormatter getDateFormatter();

  abstract DateTimeFormatter getTimeFormatter();

  abstract DateTimeFormatter getTimestampFormatter();

  abstract boolean isBlankFieldNullInUnion();

  /**
   * Generate a value by figuring out the union fields
   * @param f
   * @param strValue
   * @return
   */
  protected Object valueFromUnion(Schema f, Object in) {
    Schema[] types = f.getTypes().toArray(new Schema[0]);

    // Nullable field as element of a 2-part union
    if ("null".equals(types[0].getName()))
      if (types.length == 2) {
        String strValue = Optional.ofNullable(in).orElse("").toString();
        String sVal = strValue.trim();
        if (sVal.trim().length() == 0)
          return isBlankFieldNullInUnion() ? null : sVal;
        else
          return valueFromTypeLogicalType(types[1],
              Optional.ofNullable(types[1].getLogicalType()).map(LogicalType::getName), sVal);
      } else {
        throw new IBDataException("Null unions can only have 2 fields");
      }
    return null;
  }

  protected Object valueFromTypeLogicalType(Schema f, Optional<String> lType, Object in) {
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
          return new TimeConversions.TimeMillisConversion().toLong(LocalTime.parse(strValue, getTimeFormatter()), f,
              f.getLogicalType());
        case "time-micros":
          return new TimeConversions.TimeMicrosConversion().toLong(LocalTime.parse(strValue, getTimeFormatter()), f,
              f.getLogicalType());
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
          return new TimeConversions.DateConversion().toInt(LocalDate.parse(strValue, getDateFormatter()), f,
              f.getLogicalType());
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

  abstract public Schema getSchema();

  abstract Locale getLocale();

}
