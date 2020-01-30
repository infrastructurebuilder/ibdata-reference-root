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
package org.infrastructurebuilder.data.sql;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_ENTITY;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;
import static org.infrastructurebuilder.data.IBDataException.cet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javax.inject.Named;

import org.infrastructurebuilder.data.IBDataDecoratedDAO;
import org.infrastructurebuilder.data.IBSchema;
import org.infrastructurebuilder.data.schema.IBSchemaTranslator;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.URLAndCreds;
import org.infrastructurebuilder.util.config.AbstractCMSConfigurableSupplier;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.slf4j.Logger;

import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.integration.commandline.CommandLineUtils;

public class DefaultSchemaToLiquibaseTranslatorSupplier
    extends AbstractCMSConfigurableSupplier<IBSchemaTranslator<IBSchema, LiquibaseDAO>, URLAndCreds> {
  private final LiquibaseSupplier ls;

  public DefaultSchemaToLiquibaseTranslatorSupplier(@Named(IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps,
      LoggerSupplier l, LiquibaseSupplier ls) {
    super(wps, null, l);
    this.ls = requireNonNull(ls);
  }

  private DefaultSchemaToLiquibaseTranslatorSupplier(PathSupplier wps, ConfigMapSupplier cms, LoggerSupplier l,
      LiquibaseSupplier lbs) {
    super(wps, cms, l);
    this.ls = (LiquibaseSupplier) requireNonNull(lbs).configure(cms);
  }

  @Override
  public DefaultSchemaToLiquibaseTranslatorSupplier getConfiguredSupplier(ConfigMapSupplier cms) {
    return new DefaultSchemaToLiquibaseTranslatorSupplier(getWorkingPathSupplier(), cms, () -> getLog(), this.ls);
  }

  @Override
  protected DefaultSchemaToLiquibaseTranslator getInstance(PathSupplier wps, URLAndCreds in) {
    return new DefaultSchemaToLiquibaseTranslator(getLog(), ls);
  }

  public class DefaultSchemaToLiquibaseTranslator implements IBSchemaTranslator<IBSchema, LiquibaseDAO> {

    private final Logger log;
    private final LiquibaseSupplier liquibase;

    public DefaultSchemaToLiquibaseTranslator(Logger log, LiquibaseSupplier lb) {
      this.log = requireNonNull(log);
      this.liquibase = requireNonNull(lb);
    }

    @Override
    public Logger getLog() {
      return this.log;
    }

    @Override
    public Optional<String> getInboundType() {
      return IBSchemaTranslator.super.getInboundType(); // Outbound only
    }

    @Override
    public Optional<String> getOutboundType() {
      return of(LiquibaseDAO.class.getName());
    }

    @Override
    public Optional<List<IBSchema>> from(List<IBDataDecoratedDAO<IBSchema>> s) {
      return IBSchemaTranslator.super.from(s); // Outbound only
    }

    @Override
    public Optional<List<LiquibaseDAO>> to(List<IBSchema> s) {
      return of(requireNonNull(s).stream().map(this::generateChangeLog).collect(toList()));
    }

    public final LiquibaseDAO generateChangeLog(IBSchema s) {
      boolean outputDefaultCatalog = true;
      boolean outputDefaultSchema = true;
      boolean includeTablespace = true;
      String context = null; // TODO Synthesize this from the schema somehow?
      CompareControl.SchemaComparison[] schemaComparison = null;
      String snapshotTypes = null;
      String dataDir = null;
      String author = IBDATA_ENTITY;
      DiffOutputControl diffOutputControl = new DiffOutputControl(outputDefaultCatalog, outputDefaultSchema,
          includeTablespace, schemaComparison);
      Path changeLogFile = cet.withReturningTranslation(() -> Files.createTempFile("liquibase-", ".xml"))
          .toAbsolutePath();
      cet.withTranslation(
          () -> CommandLineUtils.doGenerateChangeLog(changeLogFile.toString(), this.liquibase.get().getDatabase(),
              s.getNameSpace().get(), s.getName().get(), snapshotTypes, author, context, dataDir, diffOutputControl));

      return new LiquibaseDAO(changeLogFile, s);
    }

  }

}