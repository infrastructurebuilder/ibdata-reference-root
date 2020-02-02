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

import static java.util.Optional.of;
import static org.infrastructurebuilder.IBConstants.AVRO_SCHEMA;
import static org.infrastructurebuilder.IBConstants.FAIL_ON_ERROR;

import java.util.Objects;
import java.util.Optional;

import javax.inject.Named;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.infrastructurebuilder.data.IBSchema;
import org.infrastructurebuilder.data.transform.IBDataIntermediary;
import org.infrastructurebuilder.data.transform.IBSchemaOutboundRowMapper;
import org.infrastructurebuilder.util.config.ConfigMap;

@Named(AbstractAvroRowMapper.AVRO)
public class DefaultAvroIBSchemaOutboundRowMapper extends AbstractAvroRowMapper
    implements IBSchemaOutboundRowMapper<GenericRecord> {

  public DefaultAvroIBSchemaOutboundRowMapper() {
    this(null, true);
  }

  protected DefaultAvroIBSchemaOutboundRowMapper(IBSchema outboundSourceSchema, boolean b) {
    super(outboundSourceSchema, b);
  }

  @Override
  public String getOutboundType() {
    return getType();
  }

  @Override
  public DefaultAvroIBSchemaOutboundRowMapper configure(IBSchema outboundSourceSchema, ConfigMap cm) {
    boolean foe = cm.getParsedBoolean(FAIL_ON_ERROR, true);
    return new DefaultAvroIBSchemaOutboundRowMapper(outboundSourceSchema, foe);
  }

  @Override
  public Optional<GenericRecord> map(IBDataIntermediary row) {
    Schema s = (Schema) getSchema().getDecoratedSchemaResources().get(AVRO_SCHEMA).getSchemaSupplier().get();
    GenericRecordBuilder rb = new GenericRecordBuilder(s);
    // final GenericRecord r = new GenericData.Record(s);
    Objects.requireNonNull(row).keySet().forEach(k -> {
      Field f = s.getField(k);
      if (f != null) {
        rb.set(f, row.get(k));
      } else {
        addGenericError("Unknown field " + k);
      }
    }); // FIXME mebbe we need to catch some of the RuntimeException instances
    return of(rb.build());
  }

}
