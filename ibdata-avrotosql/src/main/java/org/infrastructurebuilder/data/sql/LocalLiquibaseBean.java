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

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.configuration.ConfigurationProperty;
import liquibase.configuration.GlobalConfiguration;
import liquibase.configuration.LiquibaseConfiguration;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.OfflineConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.logging.LogService;
import liquibase.logging.LogType;
import liquibase.logging.Logger;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import liquibase.util.StringUtils;
import liquibase.util.file.FilenameUtils;
//import org.springframework.beans.factory.BeanNameAware;
//import org.springframework.beans.factory.InitializingBean;
//import org.springframework.context.ResourceLoaderAware;
//import org.springframework.core.io.Resource;
//import org.springframework.core.io.ResourceLoader;
//import org.springframework.core.io.support.ResourcePatternUtils;

import javax.inject.Named;
import javax.sql.DataSource;

import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.util.ExecutionEnabled;
import org.infrastructurebuilder.util.ExecutionResponse;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;

import static java.util.Optional.ofNullable;
import static org.infrastructurebuilder.data.IBDataException.cet;
import static org.infrastructurebuilder.util.IBUtils.nullIfBlank;

import java.io.*;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * A Spring-ified wrapper for Liquibase.
 * <p/>
 * Example Configuration:
 * <p/>
 * <p/>
 * This Spring configuration example will cause liquibase to run automatically when the Spring context is
 * initialized. It will load <code>db-changelog.xml</code> from the classpath and apply it against
 * <code>myDataSource</code>.
 * <p/>
 * <p/>
 *
 * <pre>
 * &lt;bean id=&quot;myLiquibase&quot;
 *          class=&quot;liquibase.spring.SpringLiquibase&quot;
 *          &gt;
 *
 *      &lt;property name=&quot;dataSource&quot; ref=&quot;myDataSource&quot; /&gt;
 *
 *      &lt;property name=&quot;changeLog&quot; value=&quot;classpath:db-changelog.xml&quot; /&gt;
 *
 * &lt;/bean&gt;
 *
 * </pre>
 *
 * @author Rob Schoening
 */
@Named(LocalLiquibaseBean.NAME)
public class LocalLiquibaseBean implements ExecutionEnabled<LocalLiquibaseBean, String, Integer> {

  //public class SpringLiquibase implements InitializingBean, BeanNameAware, ResourceLoaderAware {

  public static final String NAME = "local-liquibase";

  protected final Logger log = LogService.getLog(LocalLiquibaseBean.class);
  //    protected String beanName;

  //    protected ResourceLoader resourceLoader;

  protected final DataSource dataSource;
  protected final String changeLog;
  protected final String contexts;
  protected final String labels;
  protected final String tag;
  protected final ConfigMap parameters;
  protected final Optional<String> defaultSchema;
  protected final String liquibaseSchema;
  protected final String databaseChangeLogTable;
  protected final String databaseChangeLogLockTable;
  protected final String liquibaseTablespace;
  protected final boolean dropFirst;
  protected final boolean clearCheckSums;
  protected final boolean shouldRun;
  protected final Path rollbackFile;

  private final List<String> errors = new ArrayList<>();

  /**
   * Ignores classpath prefix during changeset comparison.
   * This is particularly useful if Liquibase is run in different ways.
   *
   * For instance, if Maven plugin is used to run changesets, as in:
   * <code>
   *      &lt;configuration&gt;
   *          ...
   *          &lt;changeLogFile&gt;path/to/changelog&lt;/changeLogFile&gt;
   *      &lt;/configuration&gt;
   * </code>
   *
   * And {@link SpringLiquibase} is configured like:
   * <code>
   *     SpringLiquibase springLiquibase = new SpringLiquibase();
   *     springLiquibase.setChangeLog("classpath:path/to/changelog");
   * </code>
   *
   * or, in equivalent XML configuration:
   * <code>
   *     &lt;bean id="springLiquibase" class="liquibase.integration.spring.SpringLiquibase"&gt;
   *         &lt;property name="changeLog" value="path/to/changelog" /&gt;
   *      &lt;/bean&gt;
   * </code>
   *
   * {@link Liquibase#listUnrunChangeSets(Contexts, )} will
   * always, by default, return changesets, regardless of their
   * execution by Maven.
   * Maven-executed changeset path name are not be prepended by
   * "classpath:" whereas the ones parsed via SpringLiquibase are.
   *
   * To avoid this issue, just set ignoreClasspathPrefix to true.
   */
  private final boolean ignoreClasspathPrefix = true;

  protected final boolean testRollbackOnUpdate;

  /**
   * Executed automatically when the bean is initialized.
   * @param dsp
   * @param cl
   * @param ctxs
   * @param labelsx
   * @param tagsx
   * @param paramsx
   * @param optDefaultSchema
   * @param lschemaX
   * @param dbCLTable
   * @param dbLockTbl
   * @param tablespace
   * @param isDropFirst
   * @param isClearChecksums
   * @param isShouldRun
   * @param isTestRollbackOnUpdate
   */
  public LocalLiquibaseBean(PathSupplier rollback, DataSourceSupplier dsp, String cl, String ctxs, String labelsx,
      String tagsx, ConfigMapSupplier paramsx, Optional<String> optDefaultSchema, String lschemaX, String dbCLTable,
      String dbLockTbl, String tablespace, boolean isDropFirst, boolean isClearChecksums, boolean isShouldRun,
      Optional<Boolean> isTestRollbackOnUpdate) {
    this.dataSource = dsp.get();
    this.changeLog = cl;
    this.contexts = ctxs;
    this.labels = labelsx;
    this.tag = tagsx;
    this.parameters = paramsx.get();
    this.defaultSchema = optDefaultSchema;
    this.liquibaseSchema = lschemaX;
    this.databaseChangeLogTable = dbCLTable;
    this.databaseChangeLogLockTable = dbLockTbl;
    this.liquibaseTablespace = tablespace;
    this.dropFirst = isDropFirst;
    this.clearCheckSums = isClearChecksums;
    this.shouldRun = isShouldRun;
    this.testRollbackOnUpdate = isTestRollbackOnUpdate.orElse(false);
    this.rollbackFile = rollback.get();

  }

  @Override
  public LocalLiquibaseBean configure(ConfigMapSupplier cms) {
    return this;
  }

  public boolean isDropFirst() {
    return dropFirst;
  }

  public boolean isClearCheckSums() {
    return clearCheckSums;
  }

  public String getDatabaseProductName() throws DatabaseException {
    Connection connection = null;
    Database database = null;
    String name = "unknown";
    try {
      connection = getDataSource().getConnection();
      database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
      name = database.getDatabaseProductName();
    } catch (SQLException e) {
      throw new DatabaseException(e);
    } finally {
      if (database != null) {
        database.close();
      } else if (connection != null) {
        try {
          if (!connection.getAutoCommit()) {
            connection.rollback();
          }
          connection.close();
        } catch (SQLException e) {
          log.warning(LogType.LOG, "problem closing connection", e);
        }
      }
    }
    return name;
  }

  /**
   * The DataSource that liquibase will use to perform the migration.
   */
  public DataSource getDataSource() {
    return dataSource;
  }

  /**
   * Returns a Resource that is able to resolve to a file or classpath resource.
   */
  public String getChangeLog() {
    return changeLog;
  }

  /**
   * Sets a Spring Resource that is able to resolve to a file or classpath resource.
   * An example might be <code>classpath:db-changelog.xml</code>.
   */
  public String getContexts() {
    return contexts;
  }

  public String getLabels() {
    return labels;
  }

  public String getTag() {
    return tag;
  }

  public Optional<String> getDefaultSchema() {
    return defaultSchema;
  }

  public String getLiquibaseTablespace() {
    return liquibaseTablespace;
  }

  public String getLiquibaseSchema() {
    return liquibaseSchema;
  }

  public String getDatabaseChangeLogTable() {
    return databaseChangeLogTable;
  }

  public String getDatabaseChangeLogLockTable() {
    return databaseChangeLogLockTable;
  }

  /**
   * Returns whether a rollback should be tested at update time or not.
   */
  public boolean isTestRollbackOnUpdate() {
    return testRollbackOnUpdate;
  }

  //    /**
  //     * If testRollbackOnUpdate is set to true a rollback will be tested at tupdate time.
  //     * For doing so when the update is performed
  //     * @param testRollbackOnUpdate
  //     */
  //    public void setTestRollbackOnUpdate(boolean testRollbackOnUpdate) {
  //        this.testRollbackOnUpdate = testRollbackOnUpdate;
  //    }

  public ExecutionResponse<String, Integer> execute() {
    ConfigurationProperty shouldRunProperty = LiquibaseConfiguration.getInstance()
        .getProperty(GlobalConfiguration.class, GlobalConfiguration.SHOULD_RUN);

    if (!shouldRunProperty.getValue(Boolean.class)) {
      return new LocalLiquibaseBeanExectionResponse(-1, Arrays.asList("Liquibase did not run because "
          + LiquibaseConfiguration.getInstance().describeValueLookupLogic(shouldRunProperty) + " was set to false"));
    }
    if (!shouldRun) {
      return new LocalLiquibaseBeanExectionResponse(-1, Arrays.asList(
          "Liquibase did not run because 'shouldRun' property was set " + "to false on Liquibase Component." + NAME));
    }

    Liquibase liquibase = null;
    try {
      liquibase = createLiquibase(getDataSource().getConnection());
      generateRollbackFile(liquibase);
      performUpdate(liquibase);
    } catch (SQLException | LiquibaseException e) {
      throw new IBDataException(e);
    } finally {
      Database database = null;
      if (liquibase != null) {
        database = liquibase.getDatabase();
      }
      if (database != null) {
        try {
          database.close();
        } catch (DatabaseException e) {
          // Error but do nothing?
          e.printStackTrace();
        }
      }
    }

    return new LocalLiquibaseBeanExectionResponse(0, errors);
  }

  private void generateRollbackFile(Liquibase liquibase) throws LiquibaseException {
    if (rollbackFile != null) {

      try (
          OutputStream fileOutputStream = Files.newOutputStream(rollbackFile, StandardOpenOption.CREATE,
              StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
          Writer output = new OutputStreamWriter(fileOutputStream,
              LiquibaseConfiguration.getInstance().getConfiguration(GlobalConfiguration.class).getOutputEncoding())

      ) {

        if (tag != null) {
          liquibase.futureRollbackSQL(tag, new Contexts(getContexts()), new LabelExpression(getLabels()), output);
        } else {
          liquibase.futureRollbackSQL(new Contexts(getContexts()), new LabelExpression(getLabels()), output);
        }
      } catch (IOException e) {
        throw new LiquibaseException("Unable to generate rollback file.", e);
      }
    }
  }

  protected void performUpdate(Liquibase liquibase) throws LiquibaseException {
    if (isClearCheckSums()) {
      liquibase.clearCheckSums();
    }

    if (isTestRollbackOnUpdate()) {
      if (tag != null) {
        liquibase.updateTestingRollback(tag, new Contexts(getContexts()), new LabelExpression(getLabels()));
      } else {
        liquibase.updateTestingRollback(new Contexts(getContexts()), new LabelExpression(getLabels()));
      }
    } else {
      if (tag != null) {
        liquibase.update(tag, new Contexts(getContexts()), new LabelExpression(getLabels()));
      } else {
        liquibase.update(new Contexts(getContexts()), new LabelExpression(getLabels()));
      }
    }
  }

  protected Liquibase createLiquibase(Connection c) throws LiquibaseException {
    ResourceAccessor resourceAccessor = createResourceOpener();
    Liquibase liquibase = new Liquibase(getChangeLog(), resourceAccessor, createDatabase(c, resourceAccessor));
    liquibase.setIgnoreClasspathPrefix(isIgnoreClasspathPrefix());
    if (parameters != null) {
      for (String entry : parameters.keySet()) {
        liquibase.setChangeLogParameter(entry, parameters.getObject(entry));
      }
    }

    if (isDropFirst()) {
      liquibase.dropAll();
    }

    return liquibase;
  }

  public final static Function<String, Optional<String>> optionalIfBlank = (s) -> ofNullable(nullIfBlank.apply(s));

  /**
   * Subclasses may override this method add change some database settings such as
   * default schema before returning the database object.
   *
   * @param c
   * @return a Database implementation retrieved from the {@link DatabaseFactory}.
   * @throws DatabaseException
   */
  protected Database createDatabase(Connection c, ResourceAccessor resourceAccessor) throws DatabaseException {

    DatabaseConnection liquibaseConnection;
    if (c == null) {
      String str = "Null connection returned by liquibase datasource. Using offline unknown database";
      log.warning(LogType.LOG, str);
      this.errors.add(str);
      liquibaseConnection = new OfflineConnection("offline:unknown", resourceAccessor);

    } else {
      liquibaseConnection = new JdbcConnection(c);
    }

    Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(liquibaseConnection);
    getDefaultSchema().ifPresent(s -> {
      if (database.supportsSchemas()) {
        cet.withTranslation(() -> database.setDefaultSchemaName(s));
      } else if (database.supportsCatalogs()) {
        cet.withTranslation(() -> database.setDefaultCatalogName(s));
      }

    });
    optionalIfBlank.apply(this.liquibaseSchema).ifPresent(s -> {
      if (database.supportsSchemas()) {
        database.setLiquibaseSchemaName(s);
      } else if (database.supportsCatalogs()) {
        database.setLiquibaseCatalogName(s);
      }
    });
    optionalIfBlank.apply(this.liquibaseTablespace).ifPresent(s -> {
      if (database.supportsTablespaces()) {
        database.setLiquibaseTablespaceName(s);
      }
    });
    optionalIfBlank.apply(this.databaseChangeLogTable).ifPresent(s -> {
      database.setDatabaseChangeLogTableName(s);
    });
    optionalIfBlank.apply(this.databaseChangeLogLockTable).ifPresent(s -> {
      database.setDatabaseChangeLogLockTableName(s);
    });
    return database;
  }

  /**
   * Create a new resourceOpener.
   */
  protected ResourceAccessor createResourceOpener() {
    return new ClassLoaderResourceAccessor(getClass().getClassLoader());

    //        return new SpringResourceOpener(getChangeLog());
  }

  public boolean isIgnoreClasspathPrefix() {
    return ignoreClasspathPrefix;
  }

  private final class LocalLiquibaseBeanExectionResponse implements ExecutionResponse<String, Integer> {

    private List<String> errors;
    private int response;

    public LocalLiquibaseBeanExectionResponse(int r, List<String> e) {
      this.errors = e;
      this.response = r;
    }

    @Override
    public List<String> getErrors() {
      return errors;
    }

    @Override
    public Integer getResponseValue() {
      return response;
    }

  }
}
