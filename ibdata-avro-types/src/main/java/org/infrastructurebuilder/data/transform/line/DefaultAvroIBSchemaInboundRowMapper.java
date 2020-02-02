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
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.infrastructurebuilder.IBConstants.FAIL_ON_ERROR;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericRecord;
import org.infrastructurebuilder.data.DefaultIBDataTransformationError;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBSchema;
import org.infrastructurebuilder.data.transform.DefaultIBDataIntermediary;
import org.infrastructurebuilder.data.transform.IBDataIntermediary;
import org.infrastructurebuilder.data.transform.IBSchemaInboundRowMapper;
import org.infrastructurebuilder.util.config.ConfigMap;

/**
 * A {@code DefaultAvroIBSchemaInboundRowMapper} is an
 * {@link IBSchemaInboundRowMapper} that handles Avro records. Specically, it
 * deals with all records as if they were themselves a map of {@code String}
 * names to arbitrary values.
 *
 * Fortunately, Avro records themselves work this way normally, with typed data
 * emerging from the other side. This means that the translatory for type can,
 * initially, be a pass thru.
 *
 * @author mykel.alvis
 *
 */
@Named(AbstractAvroRowMapper.AVRO)
public class DefaultAvroIBSchemaInboundRowMapper extends AbstractAvroRowMapper
    implements IBSchemaInboundRowMapper<GenericRecord> {


  @Inject
  public DefaultAvroIBSchemaInboundRowMapper() {
    this(null, true);
  }

  private DefaultAvroIBSchemaInboundRowMapper(IBSchema in, boolean failOnError) {
    super(in, failOnError);
  }

  @Override
  public String getInboundType() {
    return getType();
  }

  @Override
  public DefaultAvroIBSchemaInboundRowMapper configure(IBSchema inboundTargetSchema, ConfigMap cm) {
    boolean foe = cm.getParsedBoolean(FAIL_ON_ERROR, true);
    return new DefaultAvroIBSchemaInboundRowMapper(requireNonNull(inboundTargetSchema), foe);
  }

  @Override
  public Optional<IBDataIntermediary> map(GenericRecord row) {
    Schema inSchema = requireNonNull(row).getSchema();
    List<Field> fields = inSchema.getFields();
    IBDataIntermediary r = new DefaultIBDataIntermediary(getSchema());
    for (Field field : fields) {
      Object v;
      if (field.schema().getElementType().getType().equals(Schema.Type.RECORD)) {
        addError(new DefaultIBDataTransformationError(of(new IBDataException("Subrecords are currently unsupported")),
            empty()));
        throw new IBDataException("Boom"); // TODO remove this when record processing works
      } else
        v = row.get(field.name());
      // TODO Maybe we have to put translations here for Avro types, but not right now
      // For now, we just store the value as the Avro returned object type
      r.put(field.schema().getName(), v);
    }
    r = validate(r);
    return ofNullable(getCurrentErrorsList().size() > 0 ? null : r);
  }

  protected IBDataIntermediary validate(IBDataIntermediary r) {
    // check to see that all the fields in r are valid-ish
    // TODO Check types
    getSchema().getSchemaFields().forEach(sf -> {
      if (!sf.isNullable() && r.get(sf.getName()) == null) {
        addGenericError("Field " + sf.getName() + " is not nullable");
      }
      if (sf.isDeprecated() && r.containsKey(sf.getName())) {
        addGenericError("Field " + sf.getName() + " is deprecated");
      }
    });
    return r;
  }

}
