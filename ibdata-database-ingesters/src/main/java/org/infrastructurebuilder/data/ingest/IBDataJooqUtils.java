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
package org.infrastructurebuilder.data.ingest;

import static java.util.Optional.ofNullable;
import static org.apache.avro.SchemaBuilder.builder;
import static org.apache.avro.SchemaBuilder.nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.util.IBUtils;
import org.jooq.DataType;
import org.jooq.Record;
import org.jooq.Result;
import org.slf4j.Logger;

public final class IBDataJooqUtils {

  public static Field getFieldFromType(String key, org.jooq.Field<?> field, boolean isNullable) {
    Schema schema, logicalField;
    Field f1;
    DataType<?> dt = field.getDataType();
    Optional<String> comment = ofNullable(IBUtils.nullIfBlank.apply(field.getComment())); // FIXME Apply the comment?
    if (dt.getCastTypeName().equals("boolean")) {
      f1 = new Field(key, isNullable ? nullable().booleanType() : builder().booleanType());
    } else if (dt.getCastTypeName().equals("long")) {
      f1 = new Field(key, isNullable ? nullable().longType() : builder().longType());
    } else if (dt.getCastTypeName().equals("integer")) {
      f1 = new Field(key, isNullable ? nullable().intType() : builder().intType());
    } else

    if (dt.isArray()) {
      // FIXME Make Arrays work
      throw new IBDataException("Array Type " + dt + " of field '" + field.getName() + "' cannot be processed");
    } else if (dt.isDate()) {
      logicalField = Schema.create(Schema.Type.INT);
      schema = LogicalTypes.date().addToSchema(logicalField);
      f1 = new Field(key, isNullable ? nullable().type(schema) : schema);
    } else if (dt.isDateTime()) {
      logicalField = Schema.create(Schema.Type.LONG);
      schema = LogicalTypes.timeMillis().addToSchema(logicalField);
      f1 = new Field(key, isNullable ? nullable().type(schema) : schema);
    } else if (dt.isEnum()) {
      // FIXME Make Enums work
      throw new IBDataException("Enum Type " + dt + " of field '" + field.getName() + "' cannot be processed");
    } else if (dt.isInterval()) {
      // FIXME Make Intervals work
      throw new IBDataException("Interval Type " + dt + " of field '" + field.getName() + "' cannot be processed");
    } else if (dt.isLob()) {
      // FIXME Make LOBs work
      throw new IBDataException("xLOB Type " + dt + " of field '" + field.getName() + "' cannot be processed");
    } else if (dt.isNumeric()) {
      // TODO Acquire additional numeric types instead of just long
      f1 = new Field(key, isNullable ? nullable().longType() : builder().longType());
    } else if (dt.isString()) {
      f1 = new Field(key, isNullable ? nullable().stringType() : builder().stringType());
    } else if (dt.isTemporal()) {
      // FIXME Make Temporals work
      throw new IBDataException("Temporal Type " + dt + " of field '" + field.getName() + "' cannot be processed");
    } else if (dt.isTime()) {
      // FIXME Make Times work
      throw new IBDataException("Time Type " + dt + " of field '" + field.getName() + "' cannot be processed");
    } else if (dt.isTimestamp()) {
      // FIXME Make Timestamps work
      throw new IBDataException("Timestamp Type " + dt + " of field '" + field.getName() + "' cannot be processed");
    } else if (dt.isUDT()) {
      // FIXME Make UDTs work
      throw new IBDataException("UDT Type " + dt + " of field '" + field.getName() + "' cannot be processed");
    } else
      throw new IBDataException("Type " + dt + " of field '" + field.getName() + "' cannot be processed");
    return f1;
  }

  public final static Schema schemaFromRecordResults(Logger log, String namespace, String name, String doc,
      Result<Record> records) {
    log.warn("Reading entire dataset (" + records.size() + " records) to determine schema.");

    List<String> names = new ArrayList<>();
    Set<String> nullFields = new HashSet<>();
    int i;
    int row = 0;
    String key;
    org.jooq.Field<?> field;
    Record r = null;
    for (Record r2d2 : records) {
      row++;
      r = r2d2; // For last-record processing
      if (r.size() == 0)
        throw new IBDataException("No fields in record");
      for (i = 0; i < r.size(); ++i) {
        field = r.field(i);
        key = field.getName();
        if (r.getValue(i) == null) {
          if (nullFields.add(key)) {
            log.debug("Field '" + key + "' contains null value at row " + row);
          }
        }
      }
    }
    // r was the last record read
    if (r == null)
      throw new IBDataException("No records read");
    Map<String, org.apache.avro.Schema.Field> fields = new HashMap<>();
    for (i = 0; i < r.size(); ++i) {
      field = r.field(i);
      key = field.getName();
      names.add(key);
      fields.put(key, getFieldFromType(key, field, nullFields.contains(key)));
    }

    List<Field> l = names.stream().map(fields::get).collect(Collectors.toList());
    return Schema.createRecord(name, doc, namespace, false, l);
  }

}
