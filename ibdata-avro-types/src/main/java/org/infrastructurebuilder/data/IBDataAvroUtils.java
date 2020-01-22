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

import static org.apache.avro.SchemaBuilder.builder;
import static org.apache.avro.SchemaBuilder.nullable;

import java.nio.file.Path;

import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.Schema.Field;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.infrastructurebuilder.data.model.SchemaField;
import org.infrastructurebuilder.data.schema.IBSchemaTranslator;
import org.joor.Reflect;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

public interface IBDataAvroUtils extends IBSchemaTranslator<Schema,Schema> {
  public static final String NO_SCHEMA_CONFIG_FOR_MAPPER = "No schema config for mapper-";
  public final static String JAR_PREFIX = "jar:"; // TODO Move to IBConstants next core release

  /**
   * Produces a DataFileWriter for the schema provided. If supplied, a GenericData
   * will be used for translation.
   *
   * @param targetPath  The file to write
   * @param s           The schema to use in the output file
   * @param genericData Optional GenericData. If not provided a default
   *                    GenericData instannnce will be used with no converters.
   * @return DataFileWriter to write records
   */
  DataFileWriter<GenericRecord> fromSchemaAndPathAndTranslator(Path targetPath, Schema s);

  Schema avroSchemaFromString(String schema);

  DataFileWriter<GenericRecord> getGenericRecordWriterFrom(Path workingPath, String schema);

  GenericData getGenericData();

  Logger getLog();

  public static SchemaField toIBSchemaField(Schema.Field field) {
    SchemaField f = new SchemaField();
    Metadata m = new Metadata();
    f.setDeprecated(false);
    f.setDescription(field.doc());
    f.setName(field.name());
    f.setNullable(field.schema().isNullable());
    f.setMetadata(m);
    f.setTransientStructuredFieldMetadata(null);
    if (field.aliases().size() > 0) {
      Xpp3Dom aliases = new Xpp3Dom("aliases");
      field.aliases().forEach(a -> {
        Xpp3Dom alias = new Xpp3Dom("alias");
        alias.setValue(a);
        aliases.addChild(alias);
      });
      m.addChild(aliases);
    }
    if (field.hasDefaultValue()) {
      String actualDefault;
      try {
      JsonNode j = Reflect.on(field).get("defaultValue");
      actualDefault = j.toPrettyString();
      } catch (Throwable t) {
        actualDefault = "*FAILED_TO_OBTAIN*|" + t.getClass() + "|" + t.getMessage();
      }
      Xpp3Dom defaultValue = new Xpp3Dom("defaultValue");
      defaultValue.setValue(actualDefault);
      m.addChild(defaultValue);
    }
    return f;
  }

  public static Field toAvroField(IBField f) {
    Field f1 = null;
    Schema schema;
    boolean isNullable = f.isNullable();
    String doc = f.getDescription() + " Since " + f.getVersionAppeared();
    String key = f.getName();
    switch (f.getMdType().orElseThrow(() -> new IBDataException("Translation unavailable for " + f.getType()))) {
    case BOOLEAN:
      f1 = new Field(key, isNullable ? nullable().booleanType() : builder().booleanType(), doc);
      break;
    case BYTES:
      f1 = new Field(key, isNullable ? nullable().bytesType() : builder().bytesType(), doc);
      break;
    case DATE:
      schema = LogicalTypes.date().addToSchema(Schema.create(Schema.Type.INT));
      f1 = new Field(key, isNullable ? nullable().type(schema) : schema, doc);
      break;
    case DOUBLE:
      f1 = new Field(key, isNullable ? nullable().doubleType() : builder().doubleType(), doc);
      break;
    case ENUM:
      schema = SchemaBuilder.enumeration(key).symbols(f.getEnumerations().toArray(new String[0]));
      f1 = new Field(key, isNullable ? nullable().type(schema) : schema, doc);
      break;
    case FLOAT:
      f1 = new Field(key, isNullable ? nullable().floatType() : builder().floatType(), doc);
      break;
    case UINT: // Avro doesn't have unsigned types
    case INT:
      f1 = new Field(key, isNullable ? nullable().intType() : builder().intType(), doc);
      break;
    case KEY:
      f1 = new Field(key, isNullable ? nullable().stringType() : builder().stringType(), doc);
      break;
    case ULONG:
    case LONG:
      f1 = new Field(key, isNullable ? nullable().longType() : builder().longType(), doc);
      break;
    case STRING:
      f1 = new Field(key, isNullable ? nullable().stringType() : builder().stringType(), doc);
      break;
    case TIMESTAMP:
      schema = LogicalTypes.timeMillis().addToSchema(Schema.create(Schema.Type.INT));
      f1 = new Field(key, isNullable ? nullable().type(schema) : schema, doc);
      break;
    case NLDELIMITEDJSON:
    case NLDELIMITEDSTRINGS:
    case REF:
    case STREAM:
    case UNKNOWN:
    case CONST:
    default:
      throw new IBDataException("Cannot currently handle " + f.getType());
    }
    return f1;
  }


}
