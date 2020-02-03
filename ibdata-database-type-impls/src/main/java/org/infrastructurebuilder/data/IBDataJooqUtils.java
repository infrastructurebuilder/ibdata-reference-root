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

import static java.util.Collections.emptyMap;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.infrastructurebuilder.IBConstants.DBUNIT_DTD;
import static org.infrastructurebuilder.IBConstants.IBSCHEMA_MIME_TYPE;
import static org.infrastructurebuilder.data.IBDataConstants.JDBC_TYPE_NAME;
import static org.infrastructurebuilder.data.IBDataException.cet;
import static org.infrastructurebuilder.data.IBDataStructuredDataMetadataType.BOOLEAN;
import static org.infrastructurebuilder.data.IBDataStructuredDataMetadataType.BYTES;
import static org.infrastructurebuilder.data.IBDataStructuredDataMetadataType.DATE;
import static org.infrastructurebuilder.data.IBDataStructuredDataMetadataType.DOUBLE;
import static org.infrastructurebuilder.data.IBDataStructuredDataMetadataType.ENUM;
import static org.infrastructurebuilder.data.IBDataStructuredDataMetadataType.FLOAT;
import static org.infrastructurebuilder.data.IBDataStructuredDataMetadataType.INT;
import static org.infrastructurebuilder.data.IBDataStructuredDataMetadataType.KEY;
import static org.infrastructurebuilder.data.IBDataStructuredDataMetadataType.LONG;
import static org.infrastructurebuilder.data.IBDataStructuredDataMetadataType.STRING;
import static org.infrastructurebuilder.data.IBDataStructuredDataMetadataType.TIMESTAMP;
import static org.infrastructurebuilder.data.IBDataStructuredDataMetadataType.UNKNOWN;
import static org.infrastructurebuilder.data.IBSchema.SCHEMA_IO_TYPE;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatDtdDataSet;
import org.infrastructurebuilder.IBConstants;
import org.infrastructurebuilder.data.model.PersistedIBSchema;
import org.infrastructurebuilder.data.model.SchemaField;
import org.infrastructurebuilder.data.model.SchemaIndex;
import org.infrastructurebuilder.data.model.StructuredFieldMetadata;
import org.infrastructurebuilder.data.schema.IBSchemaTranslator;
import org.infrastructurebuilder.data.type.BooleanIBDataType;
import org.infrastructurebuilder.data.type.BytesIBDataType;
import org.infrastructurebuilder.data.type.DateIBDataType;
import org.infrastructurebuilder.data.type.DoubleIBDataType;
import org.infrastructurebuilder.data.type.EnumIBDataType;
import org.infrastructurebuilder.data.type.FloatIBDataType;
import org.infrastructurebuilder.data.type.IntIBDataType;
import org.infrastructurebuilder.data.type.KeyIBDataType;
import org.infrastructurebuilder.data.type.LongIBDataType;
import org.infrastructurebuilder.data.type.StringIBDataType;
import org.infrastructurebuilder.data.type.TimestampIBDataType;
import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.files.DefaultIBResource;
import org.infrastructurebuilder.util.files.IBResource;
import org.jooq.Catalog;
import org.jooq.DataType;
import org.jooq.EnumType;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.tools.reflect.Reflect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface IBDataJooqUtils extends IBSchemaTranslator<Result<Record>, Xpp3Dom> {
  public final static String INBOUND_TYPE = Result.class.getName() + "<" + Record.class.getName() + "<";
  public final static Logger log = LoggerFactory.getLogger(IBDataJooqUtils.class);


//  public static Field<?> getFieldFromType(String key, org.jooq.Field<?> field, DataType<?> dt, boolean isNullable) {
//    log.trace("key = {}, field = {}, dt = {}, isNullable = {}", key, field, dt, isNullable);
//    Schema schema, logicalField;
//    Field f1;
//    JDBCType jdbcType = JDBCType.valueOf(dt.getSQLType());
//    Optional<String> comment = ofNullable(IBUtils.nullIfBlank.apply(field.getComment())); // FIXME Apply the comment?
//    String castType = dt.getCastTypeName();
//    if (dt.getCastTypeName().equals("boolean")) {
//      f1 = new Field(key, isNullable ? nullable().booleanType() : builder().booleanType());
//    } else if (dt.getCastTypeName().equals("bigint")) {
//      f1 = new Field(key, isNullable ? nullable().longType() : builder().longType());
//    } else if (dt.getCastTypeName().equals("decimal_integer")) {
//      f1 = new Field(key, isNullable ? nullable().longType() : builder().longType());
//    } else if (dt.getCastTypeName().equals("integer")) {
//      f1 = new Field(key, isNullable ? nullable().intType() : builder().intType());
//    } else if (dt.isString() || dt.isEnum() /* FIXME see isEnum() below in comments */) {
//      f1 = new Field(key, isNullable ? nullable().stringType() : builder().stringType());
//    } else
//    // Non primitive-esqe types.
//    if (dt.isArray()) {
//      schema = SchemaBuilder.array()
//          .items(getFieldFromType(key, field, Reflect.on(dt).get("elementType"), false).schema());
//      if (isNullable) {
//        schema = SchemaBuilder.builder().unionOf().nullType().and().type(schema).endUnion();
//      }
//      f1 = new Field(key, schema);
//    } else if (dt.isDate()) {
//      logicalField = Schema.create(Schema.Type.INT);
//      schema = LogicalTypes.date().addToSchema(logicalField);
//      f1 = new Field(key, isNullable ? nullable().type(schema) : schema);
//    } else if (dt.isDateTime() || dt.isTimestamp()) {
//      logicalField = Schema.create(Schema.Type.INT);
//      schema = LogicalTypes.timeMillis().addToSchema(logicalField);
//      f1 = new Field(key, isNullable ? nullable().type(schema) : schema);
//    } else if (dt.isInterval()) {
//      // Representation as a Long (in ms, so you can map to Duration::ofMillis)
//      schema = Schema.create(Schema.Type.LONG);
//      f1 = new Field(key, isNullable ? nullable().type(schema) : schema);
//    } else if (dt.isTime()) {
//      logicalField = Schema.create(Schema.Type.INT);
//      schema = LogicalTypes.timeMillis().addToSchema(logicalField);
//      f1 = new Field(key, isNullable ? nullable().type(schema) : schema);
//    } else if (dt.isNumeric()) {
//      // We could acquire additional numeric types instead of just long,
//      // but AFAIK this code is unreachable
//      // because only int and long are Numerics
//      // f1 = new Field(key, isNullable ? nullable().longType() :
//      // builder().longType());
//      throw new IBDataException(
//          "NUMERIC Type " + dt + "/" + castType + " of field '" + field.getName() + "' cannot be processed");
//    } else if (dt.isUDT()) {
//      UDTRecord<?> record = (UDTRecord<?>) dt;
//      // FIXME Make UDTs work. Somehow it could become a record
//      throw new IBDataException("UDT Type " + dt + " of field '" + field.getName() + "' cannot be processed");
//
//      // All enumeration types will be strings in the generated avro
//      // } else if (dt.isEnum()) {
//      // Force all enumerations to be strings
//      // EnumType t = (EnumType) dt;
//      // Catalog q = t.getCatalog();
//      // Optional<org.jooq.Schema> s = Optional.ofNullable(t.getSchema());
//      // List<String> symbols = new ArrayList<>();
//      // schema = SchemaBuilder.enumeration(key).symbols(symbols.toArray(new
//      // String[0]));
//      // f1 = new Field(key, isNullable ? nullable().type(schema) : schema);
//      // TODO If we can figure out how to get the symbols for the enumeration, this
//      // will work better
//      // f1 = new Field(key, isNullable ? nullable().stringType() :
//      // builder().stringType());
//    } else if (dt.isLob()) {
//      // FIXME Make LOBs work
//      throw new IBDataException("xLOB Type " + dt + " of field '" + field.getName() + "' cannot be processed");
//    } else
//      throw new IBDataException("Type " + dt + " of field '" + field.getName() + "' cannot be processed");
//    return f1;
//  }

//  public static Schema avroSchemaFromRecordResults(Logger log, String namespace, String name, String doc,
//      Result<Record> records) {
//    log.warn("Reading entire dataset (" + records.size() + " records) to determine schema.");
//
//    List<String> names = new ArrayList<>();
//    Set<String> nullFields = new HashSet<>();
//    int i;
//    int row = 0;
//    String key;
//    org.jooq.Field<?> field;
//    Record r = null;
//    for (Record r2d2 : records) {
//      row++;
//      r = r2d2; // For last-record processing
//      if (r.size() == 0)
//        throw new IBDataException("No fields in record");
//      for (i = 0; i < r.size(); ++i) {
//        field = r.field(i);
//        key = field.getName();
//        if (r.getValue(i) == null) {
//          if (nullFields.add(key)) {
//            log.debug("Field '" + key + "' contains null value at row " + row);
//          }
//        }
//      }
//    }
//    // r was the last record read
//    if (r == null)
//      throw new IBDataException("No records read");
//    Map<String, org.apache.avro.Schema.Field> fields = new HashMap<>();
//    for (i = 0; i < r.size(); ++i) {
//      field = r.field(i);
//      key = field.getName();
//      names.add(key);
//      fields.put(key, getFieldFromType(key, field, field.getDataType(), nullFields.contains(key)));
//    }
//
//    List<Field> l = names.stream().map(fields::get).collect(toList());
//    return Schema.createRecord(name, doc, namespace, false, l);
//  }


  public static SchemaField getIBFieldFromType(int index, String key, org.jooq.Field<?> field, DataType<?> dt,
      boolean isNullable, String thisVersion, Optional<Object> minIn, Optional<Object> maxIn, int uniques) {
    log.trace("key = {}, field = {}, dt = {}, isNullable = {}", key, field, dt, isNullable);
    SchemaField f1 = new SchemaField();

    StructuredFieldMetadata sfmd = new StructuredFieldMetadata();
    sfmd.setIndex(index);
    sfmd.setUniqueValuesCount(Integer.toString(uniques));

    f1.setDeprecated(false);
    f1.setIndex(index);
    f1.setName(key);
    f1.setDescription(IBUtils.nullIfBlank.apply(field.getComment()));
    f1.setNullable(isNullable);
    f1.setVersionAppeared(thisVersion);
    f1.setVersionDeprecated(null);
    Metadata md = new Metadata();

    JDBCType jdbcType = JDBCType.valueOf(dt.getSQLType());
    Xpp3Dom jDom = new Xpp3Dom(JDBC_TYPE_NAME);
    jDom.setValue(jdbcType.name());
    md.addChild(jDom);
    switch (jdbcType) {
    case BIT:
    case BOOLEAN:
      f1.setType(BooleanIBDataType.TYPE);
      break;
    case NUMERIC:
    case DECIMAL:
      sfmd.setMin(minIn.map(Object::toString).map(Long::parseLong).map(l -> l.toString()).orElse(null));
      sfmd.setMax(maxIn.map(Object::toString).map(Long::parseLong).map(l -> l.toString()).orElse(null));
      f1.setType(LongIBDataType.TYPE);
      break;
    case FLOAT:
      sfmd.setMin(minIn.map(Object::toString).map(Float::parseFloat).map(l -> l.toString()).orElse(null));
      sfmd.setMax(maxIn.map(Object::toString).map(Float::parseFloat).map(l -> l.toString()).orElse(null));
      f1.setType(FloatIBDataType.TYPE);
      break;
    case LONGVARBINARY:
    case VARBINARY:
    case BLOB:
      f1.setType(BytesIBDataType.TYPE);
      break;
    case DOUBLE:
    case REAL:
      sfmd.setMin(minIn.map(Object::toString).map(Double::parseDouble).map(l -> l.toString()).orElse(null));
      sfmd.setMax(maxIn.map(Object::toString).map(Double::parseDouble).map(l -> l.toString()).orElse(null));
      f1.setType(DoubleIBDataType.TYPE);
      break;
    case SMALLINT:
    case BIGINT:
    case TINYINT:
    case INTEGER:
      sfmd.setMin(minIn.map(Object::toString).map(Integer::parseInt).map(l -> l.toString()).orElse(null));
      sfmd.setMax(maxIn.map(Object::toString).map(Integer::parseInt).map(l -> l.toString()).orElse(null));
      f1.setType(IntIBDataType.TYPE);
      break;
    case DATE:
      f1.setType(DateIBDataType.TYPE);
      break;
    case TIMESTAMP:
    case TIMESTAMP_WITH_TIMEZONE:
      f1.setType(TimestampIBDataType.TYPE);
      break;
    case BINARY:
    case NCLOB:
    case CLOB:
    case NCHAR:
    case NVARCHAR:
    case LONGVARCHAR:
    case LONGNVARCHAR:
      sfmd.setMin(minIn.map(Object::toString).map(String::length).map(l -> l.toString()).orElse(null));
      sfmd.setMax(maxIn.map(Object::toString).map(String::length).map(l -> l.toString()).orElse(null));
      f1.setType(StringIBDataType.TYPE);
      break;
    case CHAR:
    case VARCHAR:
      sfmd.setMin(minIn.map(Object::toString).map(String::length).map(l -> l.toString()).orElse(null));
      sfmd.setMax(maxIn.map(Object::toString).map(String::length).map(l -> l.toString()).orElse(null));
      f1.setType(StringIBDataType.TYPE);
      break;
    case ROWID:
      sfmd.setMin(minIn.map(Object::toString).map(String::length).map(l -> l.toString()).orElse(null));
      sfmd.setMax(maxIn.map(Object::toString).map(String::length).map(l -> l.toString()).orElse(null));
      f1.setType(KeyIBDataType.TYPE);
      break;
    case STRUCT:
    case ARRAY:
      // dt.isArray()
    case TIME:
    case TIME_WITH_TIMEZONE:
    case DATALINK:
    case NULL:
    case DISTINCT:
    case JAVA_OBJECT:
    case OTHER:
    case REF:
    case REF_CURSOR:
    case SQLXML:
    default:
      f1.setDeprecated(true);
      f1.setVersionDeprecated(thisVersion);
      f1.setType(UNKNOWN.name());

      if (dt.isEnum()) {
        // TODO Work on Enums
        f1.setType(EnumIBDataType.TYPE);
        EnumType t = (EnumType) dt;
        Optional<org.jooq.Schema> s = Optional.ofNullable(t.getSchema());
        Catalog q = t.getCatalog();
        List<org.jooq.Schema> eSchemas = q.getSchemas();
        List<String> symbols = new ArrayList<>();
        // TODO work on enums
        throw new IBDataException("Type " + dt + " of field '" + field.getName() + "' cannot be processed");
      } else {
        throw new IBDataException("Type " + dt + " of field '" + field.getName() + "' cannot be processed");
      }
    }
    f1.setTransientStructuredFieldMetadata(sfmd);
    f1.setMetadata(md);
    return f1;
  }

  public static IBSchema ibSchemaFromRecordResults(Logger log, String namespace, String name, String doc,
      String jdbcURL, Result<Record> records, String thisVersion) {

//    Schema s = avroSchemaFromRecordResults(log, namespace, name, doc, records);
    log.warn("Reading entire dataset (" + records.size() + " records) to determine schema.");
    PersistedIBSchema schema = new PersistedIBSchema();
    schema.setName(name);
    schema.setDescription(doc);
    schema.setCreationDate(new Date());
    schema.setNameSpace(namespace);
    schema.setUrl(jdbcURL);
    schema.setIndexes(new ArrayList<>());
    schema.setMimeType(IBConstants.IBDATA_SCHEMA);
    Metadata md = new Metadata();

    List<String> names = new ArrayList<>();
    int i;
    int row = 0;
    String key;
    org.jooq.Field<?> field;
    Record r = null;
    Map<Integer, StructuredFieldMetadata> sfmd = new HashMap<>();
    Map<Integer, Object> mins = new HashMap<>();
    Map<Integer, Object> maxs = new HashMap<>();
    SortedSet<Integer> nullFields = new TreeSet<>();
    Map<Integer, Map<Object, Integer>> uniqueValuesCounts = new HashMap<>();
    for (Record r2d2 : records) {
      row++;
      r = r2d2; // For last-record processing
      if (r.size() == 0)
        throw new IBDataException("No fields in record");
      for (i = 0; i < r.size(); ++i) {
        field = r.field(i);
        key = field.getName();
        DataType<?> type = field.getDataType();
        JDBCType jdbcType = JDBCType.valueOf(type.getSQLType());
        Class<?> javaType = type.getType();
        Object val = r.getValue(i);
        String scale = null;
        try {
          Object currentMin = mins.computeIfAbsent(i, k -> val);
          Object currentMax = maxs.computeIfAbsent(i, k -> val);
          if (val instanceof String) {
            String l = (String) val;
            String m = (String) currentMin;
            String q = (String) currentMax;
            if (l.length() < m.length())
              currentMin = l;
            if (l.length() > q.length())
              currentMax = l;
          } else if (val.getClass().isArray()) {
            throw new IBDataException("Arrays are not currently managebel");
          } else {
            int x = Reflect.on(val).call("compareTo", currentMin).get();
            if (x < 0)
              currentMin = val;
            int y = Reflect.on(val).call("compareTo", currentMax).get();
            if (y > 0)
              currentMax = val;
          }
        } catch (Exception e) {
          // Do nothing, ignoring the results except for computeIfAbsent
        }

        Map<Object, Integer> unique = uniqueValuesCounts.computeIfAbsent(i, k -> new HashMap<>());
        unique.computeIfAbsent(val, v -> new Integer(0));
        unique.computeIfPresent(val, (v, c) -> {
          return c + 1;
        });
        if (val == null) {
          if (nullFields.add(i)) {
            log.debug("Field '" + key + "' contains null value at row " + row);
          }
        }
      }
    }
    // r was the last record read
    if (r == null)
      throw new IBDataException("No records read");
    Map<String, SchemaField> fields = new HashMap<>();
    for (i = 0; i < r.size(); ++i) {
      field = r.field(i);
      key = field.getName();
      names.add(key);
      fields.put(key,
          getIBFieldFromType(i, key, field, field.getDataType(), nullFields.contains(i), thisVersion,
              ofNullable(mins.get(i)), ofNullable(maxs.get(i)),
              ofNullable(uniqueValuesCounts.get(i)).orElseGet(() -> emptyMap()).keySet().size()));
    }

    List<SchemaField> l = names.stream().map(fields::get).collect(toList());
    schema.setFields(l);
    List<SchemaIndex> indexes = new ArrayList<>();
    // TODO Get indexes somehow
    schema.setIndexes(indexes);
    schema.setMetadata(md);
    schema.setMimeType(IBConstants.IBDATA_SCHEMA);
    return schema;
  }

  @Override
  default Optional<String> getInboundType() {
    return Optional.of(INBOUND_TYPE);
  }

  @Override
  default Optional<String> getOutboundType() {
    return Optional.of(SCHEMA_IO_TYPE);
  }
}
