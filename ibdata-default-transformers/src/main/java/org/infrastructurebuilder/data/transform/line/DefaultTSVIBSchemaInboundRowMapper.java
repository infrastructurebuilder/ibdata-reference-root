package org.infrastructurebuilder.data.transform.line;

import javax.inject.Inject;
import javax.inject.Named;

@Named("tsv")
public class DefaultTSVIBSchemaInboundRowMapper extends DefaultCSVIBSchemaInboundRowMapper {

  @Inject
  public DefaultTSVIBSchemaInboundRowMapper() {
    super();
  }

  @Override
  protected char getSeparator() {
    return '\t';
  }
}
