package org.infrastructurebuilder.data.transform.line;

import javax.inject.Inject;
import javax.inject.Named;

@Named("psv")
public class DefaultPSVIBSchemaInboundRowMapper extends DefaultCSVIBSchemaInboundRowMapper {

  @Inject
  public DefaultPSVIBSchemaInboundRowMapper() {
    super();
  }

  @Override
  protected char getSeparator() {
    return '|';
  }
}
