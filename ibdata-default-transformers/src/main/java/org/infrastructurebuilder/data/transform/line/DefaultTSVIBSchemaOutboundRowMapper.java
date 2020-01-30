package org.infrastructurebuilder.data.transform.line;

import javax.inject.Inject;
import javax.inject.Named;

@Named("tsv")
public class DefaultTSVIBSchemaOutboundRowMapper extends DefaultCSVIBSchemaOutboundRowMapper {

  @Inject
  public DefaultTSVIBSchemaOutboundRowMapper() {
    super();
  }

  @Override
  protected char getSeparator() {
    return '\t';
  }

}
