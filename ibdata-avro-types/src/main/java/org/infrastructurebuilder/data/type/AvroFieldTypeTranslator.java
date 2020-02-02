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
package org.infrastructurebuilder.data.type;

import static org.apache.avro.SchemaBuilder.builder;
import static org.apache.avro.SchemaBuilder.nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.avro.LogicalTypes;
import org.apache.avro.LogicalTypes.Date;
import org.apache.avro.LogicalTypes.Decimal;
import org.apache.avro.LogicalTypes.TimeMicros;
import org.apache.avro.LogicalTypes.TimeMillis;
import org.apache.avro.LogicalTypes.TimestampMicros;
import org.apache.avro.LogicalTypes.TimestampMillis;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.SchemaBuilder;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBField;
import org.infrastructurebuilder.data.model.SchemaField;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;

@Named("avro")
public class AvroFieldTypeTranslator extends AbstractIBDataTypeTranslator<Field> {
  @SuppressWarnings("serial")
  private final static Map<Schema.Type, String> tMap = new HashMap<Schema.Type, String>() {
    {
      put(Type.BOOLEAN, BooleanIBDataType.TYPE);
      put(Type.DOUBLE, DoubleIBDataType.TYPE);
      put(Type.FLOAT, FloatIBDataType.TYPE);
      put(Type.ENUM, EnumIBDataType.TYPE);
      put(Type.STRING, StringIBDataType.TYPE);
      put(Type.INT, IntIBDataType.TYPE);
      put(Type.LONG, LongIBDataType.TYPE);
      put(Type.BYTES, BytesIBDataType.TYPE);
    }
  };

  @Inject
  public AvroFieldTypeTranslator(IBRuntimeUtils ibr) {
    super(Field.class.getName(), ibr);
  }

  @Override
  public IBField to(Field in) {
    SchemaField f = new SchemaField();
    f.setDeprecated(false);
    f.setDescription(in.doc());
    f.setName(in.name());
    f.setNullable(in.schema().isNullable());
    f.setType(typeFromType(in));
    f.setVersionAppeared(getRuntimeUtils().getWorkingGAV().getVersion().get());
    List<String> o = Optional.ofNullable(in.schema().getEnumSymbols()).orElse(Collections.emptyList());
    if (o.size() > 0) {
      f.setEnumerations(o);
    }
    return f;
  }

  public String typeFromType(Field in) {
    Schema s = in.schema();
    Optional<String> t = Optional.ofNullable(tMap.get(s.getType()));
    return Optional.ofNullable(s.getLogicalType()).map(lt -> {
      if (lt instanceof Date) {
        return DateIBDataType.TYPE;
      } else if (lt instanceof Decimal) {
        return DecimalIBDataType.TYPE;
      } else if (lt instanceof TimeMicros) {
        return TimeIBDataType.TYPE;
      } else if (lt instanceof TimeMillis) {
        return TimeIBDataType.TYPE;
      } else if (lt instanceof TimestampMicros) {
        return TimestampIBDataType.TYPE;
      } else if (lt instanceof TimestampMillis) {
        return TimestampIBDataType.TYPE;
      } else
        throw new IBDataException("Unknown Avro Logical Type " + lt.getName());
    }).orElse(t.orElseThrow(() -> new IBDataException("Unknown Avro Basic Type " + s.getType())));
  }

  @Override
  public Field from(IBField out) {
    Field f1 = null;
    Schema schema;
    boolean isNullable = out.isNullable();
    String doc = out.getDescription() + " Since " + out.getVersionAppeared();
    String key = out.getName();
    String k = out.getType();
    if (BooleanIBDataType.TYPE.equals(k)) {
      f1 = new Field(key, isNullable ? nullable().booleanType() : builder().booleanType(), doc);
    } else if (BytesIBDataType.TYPE.equals(k)) {
      f1 = new Field(key, isNullable ? nullable().bytesType() : builder().bytesType(), doc);
    } else if (DateIBDataType.TYPE.equals(k)) {
      schema = LogicalTypes.date().addToSchema(Schema.create(Schema.Type.INT));
      f1 = new Field(key, isNullable ? nullable().type(schema) : schema, doc);
    } else if (DoubleIBDataType.TYPE.equals(k)) {
      f1 = new Field(key, isNullable ? nullable().doubleType() : builder().doubleType(), doc);
    } else if (EnumIBDataType.TYPE.equals(k)) {
      schema = SchemaBuilder.enumeration(key).symbols(out.getEnumerations().toArray(new String[0]));
      f1 = new Field(key, isNullable ? nullable().type(schema) : schema, doc);
    } else if (FloatIBDataType.TYPE.equals(k)) {
      f1 = new Field(key, isNullable ? nullable().floatType() : builder().floatType(), doc);
    } else if (IntIBDataType.TYPE.equals(k) || UnsignedIntIBDataType.TYPE.equals(k)) {
      f1 = new Field(key, isNullable ? nullable().intType() : builder().intType(), doc);
    } else if (KeyIBDataType.TYPE.equals(k)) {
      f1 = new Field(key, isNullable ? nullable().stringType() : builder().stringType(), doc);
    } else if (LongIBDataType.TYPE.equals(k) || UnsignedLongIBDataType.TYPE.equals(k)) {
      f1 = new Field(key, isNullable ? nullable().longType() : builder().longType(), doc);
    } else if (StringIBDataType.TYPE.equals(k)) {
      f1 = new Field(key, isNullable ? nullable().stringType() : builder().stringType(), doc);
    } else if (TimestampIBDataType.TYPE.equals(k)) {
      schema = LogicalTypes.timeMillis().addToSchema(Schema.create(Schema.Type.INT));
      f1 = new Field(key, isNullable ? nullable().type(schema) : schema, doc);
    } else {
      throw new IBDataException("Cannot currently handle " + out.getType());
    }
    return f1;
  }

  @Override
  public IBDataTypeTranslator<Field> configure(ConfigMapSupplier cms) {
    return this;
  }

}
