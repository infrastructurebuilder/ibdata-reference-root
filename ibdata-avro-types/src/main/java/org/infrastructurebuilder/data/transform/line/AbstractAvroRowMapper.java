package org.infrastructurebuilder.data.transform.line;

import org.apache.avro.Schema;
import org.infrastructurebuilder.data.IBSchema;

abstract public class AbstractAvroRowMapper extends AbstractRowMapper {
  public static final String AVRO = "avro";

  protected AbstractAvroRowMapper(IBSchema in, boolean failOnError) {
    super(in,failOnError);
  }

  protected String getType() {
    return Schema.class.getCanonicalName();
  }

}