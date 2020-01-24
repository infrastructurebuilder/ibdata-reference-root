package org.infrastructurebuilder.data.sql;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static org.infrastructurebuilder.IBConstants.APPLICATION_LIQUIBASE_CHANGELOG;

import java.nio.file.Path;

import org.infrastructurebuilder.data.IBSchema;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.files.DefaultIBResource;

public class LiquibaseDAO extends DefaultIBResource {
  private static final long serialVersionUID = 4003966523021611318L;
  private final IBSchema schema;

  public LiquibaseDAO(Path changeLogFile, IBSchema s) {
    super(changeLogFile, new Checksum(changeLogFile), of(APPLICATION_LIQUIBASE_CHANGELOG));
    this.schema = requireNonNull(s);
  }

  public IBSchema getSchema() {
    return schema;
  }
}
