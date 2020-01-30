package org.infrastructurebuilder.data.transform.line;

import javax.inject.Inject;
import javax.inject.Named;

@Named("psv")
public class DefaultPSVIBSchemaOutboundRowMapper extends DefaultCSVIBSchemaOutboundRowMapper {

  @Inject
  public DefaultPSVIBSchemaOutboundRowMapper() {
    super();
  }

  @Override
  protected char getSeparator() {
    return '|';
  }

}
