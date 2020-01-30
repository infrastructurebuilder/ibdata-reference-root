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
package org.infrastructurebuilder.data;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static liquibase.diff.output.StandardObjectChangeFilter.FilterType.EXCLUDE;
import static liquibase.diff.output.StandardObjectChangeFilter.FilterType.INCLUDE;
import static org.dbunit.database.search.TablesDependencyHelper.getAllDependentTables;
import static org.infrastructurebuilder.IBConstants.DBUNIT_DTD;
import static org.infrastructurebuilder.IBConstants.DBUNIT_FLATXML;
import static org.infrastructurebuilder.IBConstants.DEFAULT;
import static org.infrastructurebuilder.IBConstants.DTD;
import static org.infrastructurebuilder.IBConstants.IBSCHEMA_MIME_TYPE;
import static org.infrastructurebuilder.IBConstants.LIQUIBASE_SCHEMA;
import static org.infrastructurebuilder.IBConstants.XML;
import static org.infrastructurebuilder.data.IBDataException.cet;
import static org.infrastructurebuilder.data.IBDataJooqUtils.ibSchemaFromRecordResults;
import static org.infrastructurebuilder.util.IBUtils.nullIfBlank;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.DatabaseSequenceFilter;
import org.dbunit.database.ForwardOnlyResultSetTableFactory;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.filter.ITableFilter;
import org.dbunit.dataset.xml.FlatDtdDataSet;
import org.dbunit.dataset.xml.FlatXmlWriter;
import org.infrastructurebuilder.IBConstants;
import org.infrastructurebuilder.data.model.PersistedIBSchema;
import org.infrastructurebuilder.data.model.io.xpp3.PersistedIBSchemaXpp3Writer;
import org.infrastructurebuilder.util.CredentialsFactory;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.URLAndCreds;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.artifacts.GAV;
import org.infrastructurebuilder.util.artifacts.impl.DefaultGAV;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.DefaultIBResource;
import org.infrastructurebuilder.util.files.IBResource;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.SQLDialect.ThirdParty;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.slf4j.Logger;

import liquibase.CatalogAndSchema;
import liquibase.command.CommandFactory;
import liquibase.command.core.GenerateChangeLogCommand;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.StandardObjectChangeFilter;

abstract public class AbstractIBDatabaseDriverSupplier implements IBDataDatabaseDriverSupplier {

  private static final String GENERATE_CHANGE_LOG = "generateChangeLog";
  private static final String FAILED_TO_QUERY_SCHEMA_FOR = "Failed to query schema for ";
  private final String hint;
  private final String lbDatabaseClassName;
  private final List<DefaultGAV> gavs;
  private final Database database;
  private final Logger log;
  private final CredentialsFactory cf;
  private final PathSupplier wps;

  protected AbstractIBDatabaseDriverSupplier(PathSupplier wps, LoggerSupplier l, String hint,
      String liquibaseDatabaseClass, CredentialsFactory cf, String... list) {
    this.wps = requireNonNull(wps, "Working path supplier");
    this.log = requireNonNull(l, "LoggerSupplier").get();
    this.hint = requireNonNull(hint, "Driver hint");
    this.cf = requireNonNull(cf, "CredentialsFactory");
    this.lbDatabaseClassName = requireNonNull(liquibaseDatabaseClass, "Liquibase Database Classname");
    Database db;
    try {
      db = (Database) Class.forName(lbDatabaseClassName).newInstance();
    } catch (Exception e) {
      db = null;
    }
    this.database = db;
    this.gavs = ofNullable(list).map(Arrays::asList).orElse(Collections.emptyList())
        // Map list to list of GAVs
        .stream().map(DefaultGAV::new).collect(toList());
  }

  public PathSupplier getWorkingPathSupplier() {
    return wps;
  }

  @Override
  public Logger getLog() {
    return this.log;
  }

  @Override
  public CredentialsFactory getCredentialsFactory() {
    return cf;
  }

  @Override
  public Optional<IBDatabaseDialect> getDialect(URLAndCreds jdbcUrl) {
    return getDatabaseDriverClassName(jdbcUrl).flatMap(driver -> {
      return IBDataDatabaseUtils.bySQLDialectName(getJooqName()).map(j -> {
        return new DefaultIBDatabaseDialect(j, this.lbDatabaseClassName, getDbUnitConfigurationUpdates());
      });
    });
  }

  protected final Optional<Database> getDatabase() {
    return ofNullable(this.database);
  }

  public List<GAV> getRequiredArtifacts() {
    return unmodifiableList(this.gavs);
  }

  @Override
  public String getHint() {
    return hint;
  }

  @Override
  public Optional<String> getDatabaseDriverClassName(URLAndCreds jdbcUrl) {
    return ofNullable(jdbcUrl).flatMap(u -> getDatabase().map(db -> db.getDefaultDriver(u.getUrl())));
  }

  @Override
  public boolean respondsTo(URLAndCreds jdbcURL) {
    return getDatabaseDriverClassName(jdbcURL).isPresent();
  }

  @Override
  public Optional<Supplier<DataSource>> getDataSourceSupplier(URLAndCreds u) {
    return getDatabaseDriverClassName(u)
        .map(driverClass -> () -> new IBDataConfiguredDataSource(driverClass, u, getCredentialsFactory()));
  }

  public static Map<String, IBResource> generateExportSet(Connection conn, Path workingPath, String tableName,
      Optional<String> where) {
    Map<String, IBResource> k = new HashMap<>();
    IBResource a = generateLiquibaseChangelog(conn, workingPath, of(asList(tableName)), empty());
    k.put(LIQUIBASE_SCHEMA, a);
    IBResource b = cet.withReturningTranslation(() -> generateDbunitDTDSchema(conn, workingPath).moveTo(workingPath));
    String relativeDTD = b.getPath().getFileName().toString(); // Since the name's set bu moveTo above, it'll remain set
    k.put(DBUNIT_DTD, b);
    IBResource c = generateDbunitTableExport(conn, workingPath, tableName, where, of(relativeDTD));
    k.put(DBUNIT_FLATXML, c);
    return k;
  }

  public static IBResource generateDbunitDTDSchema(Connection conn, Path workingPath) {
    Path pdtd = workingPath.resolve(UUID.randomUUID() + DTD);
    try (Writer ww = Files.newBufferedWriter(pdtd)) {
//      FlatDtdWriter datasetWriter = new FlatDtdWriter(ww);
//      datasetWriter.setContentModel(FlatDtdWriter.CHOICE);

      FlatDtdDataSet.write(new DatabaseConnection(conn).createDataSet(), ww);
    } catch (IOException | SQLException | DatabaseUnitException e) {
      throw new IBDataException(e);
    }
    return new DefaultIBResource(pdtd, new Checksum(pdtd), of(DBUNIT_DTD));
  }

  public static String generateSelectForTable(String tableName, Optional<String> where, Optional<List<String>> fields) {
    String fieldList = requireNonNull(fields).map(f -> f.stream().collect(joining(", "))).orElse("*");
    String finalWhere = requireNonNull(where).map(w -> String.format("WHERE %s", w)).orElse(" ");
    return String.format("SELECT %s FROM %s %s", fieldList, requireNonNull(tableName), finalWhere);
  }

  public static IBResource generateDbunitTableExport(Connection conn, Path workingPath, String tableName,
      Optional<String> where, Optional<String> relPathToDTD) {
    Path pdtd = workingPath.resolve(UUID.randomUUID() + XML);
    try (Writer ww = Files.newBufferedWriter(pdtd)) {
      IDatabaseConnection connection = new DatabaseConnection(conn);
      QueryDataSet partialDataSet = new QueryDataSet(connection);
      FlatXmlWriter writer = new FlatXmlWriter(ww);
      requireNonNull(relPathToDTD).ifPresent(dtd -> writer.setDocType(dtd));
      partialDataSet.addTable(requireNonNull(tableName), generateSelectForTable(tableName, where, empty()));
      // Order by keys
      ITableFilter filter = new DatabaseSequenceFilter(connection);
      writer.write(new FilteredDataSet(filter, partialDataSet));
    } catch (IOException | DatabaseUnitException | SQLException e) {
      throw new IBDataException(e);
    }
    return new DefaultIBResource(pdtd, new Checksum(pdtd), of(DBUNIT_FLATXML));
  }

  public static IBResource generateDbunitDependentExport(Connection conn, Path workingPath, String name) {
    Path pdtd = workingPath.resolve(UUID.randomUUID() + XML);
    try (Writer ww = Files.newBufferedWriter(pdtd)) {
      IDatabaseConnection connection = new DatabaseConnection(conn);
      FlatDtdDataSet.write(connection.createDataSet(getAllDependentTables(connection, requireNonNull(name))), ww);
    } catch (IOException | SQLException | DatabaseUnitException e) {
      throw new IBDataException(e);
    }
    return new DefaultIBResource(pdtd, new Checksum(pdtd), of(DBUNIT_FLATXML));
  }

  public static IBResource generateLiquibaseChangelog(Connection conn, Path workingPath,
      Optional<List<String>> includeObjects, Optional<List<String>> excludeObjects) {
    Path pdtd = workingPath.resolve(UUID.randomUUID() + XML);
//    String dataDir = workingPath.resolve(UUID.randomUUID().toString() + "-lbDataDir").toAbsolutePath().toString();
//    FileSystemResourceAccessor fsOpener = new FileSystemResourceAccessor();
//    CommandLineResourceAccessor clOpener = new CommandLineResourceAccessor(conn.getClass().getClassLoader());
//    CompositeResourceAccessor fileOpener = new CompositeResourceAccessor(fsOpener, clOpener);
    DatabaseFactory dbf = cet.withReturningTranslation(() -> DatabaseFactory.getInstance());
    Database database = cet
        .withReturningTranslation(() -> dbf.findCorrectDatabaseImplementation(new JdbcConnection(conn)));

    // Note much of this is copied straight from the liquibase codebase and then
    // reformatted. We're not covering really new ground here, and the Database
    // instance
    // has to be built using the local classpath (which is likely to be correct)

    CompareControl.ComputedSchemas computedSchemas = CompareControl.computeSchemas(null // schemas
        , null // referenceSchemas
        , null // outputSchemasAs
        , null // defaultCatalogName
        , null // defaultSchemaName
        , null // referenceDefaultCatalogName
        , null // referenceDefaultSchemaName
        , database);

    DiffOutputControl diffOutputControl = new DiffOutputControl(
        // includeCatalog
        false
        // includeSchema
        , true
        // includeTablespace
        , false, computedSchemas.finalSchemaComparisons)
            // Set Data dir to new working path value since no data loading here
            .setDataDir(null);

    ofNullable(requireNonNull(includeObjects)
        .map(i -> new StandardObjectChangeFilter(INCLUDE, i.stream().collect(joining(","))))
        .orElseGet(() -> requireNonNull(excludeObjects)
            .map(i -> new StandardObjectChangeFilter(EXCLUDE, i.stream().collect(joining(",")))).orElse(null)))
                .ifPresent(diffOutputControl::setObjectChangeFilter);
    for (CompareControl.SchemaComparison schema : computedSchemas.finalSchemaComparisons) {
      diffOutputControl.addIncludedSchema(schema.getReferenceSchema());
      diffOutputControl.addIncludedSchema(schema.getComparisonSchema());
    }

    CompareControl.SchemaComparison[] comparisons = new CompareControl.SchemaComparison[computedSchemas.finalTargetSchemas.length];
    int i = 0;
    for (CatalogAndSchema schema : computedSchemas.finalTargetSchemas) {
      comparisons[i++] = new CompareControl.SchemaComparison(schema, schema);
    }

    String diffTypes = null; // TODO Set diffTypes? Null means everything
    GenerateChangeLogCommand command = (GenerateChangeLogCommand) CommandFactory.getInstance()
        .getCommand(GENERATE_CHANGE_LOG);

    command
        // who
        .setAuthor(IBDataConstants.IBDATA_ENTITY)
        // context
        .setContext(IBConstants.DEFAULT)
        // origdb
        .setReferenceDatabase(database)
        // snapshot types
        .setSnapshotTypes(nullIfBlank.apply(diffTypes))
        // ??
        .setOutputStream(System.out)
        // comparisons control
        .setCompareControl(new CompareControl(comparisons, nullIfBlank.apply(diffTypes)));
    command
        // actual file
        .setChangeLogFile(pdtd.toString())
        // diff output control
        .setDiffOutputControl(diffOutputControl);

    cet.withTranslation(() -> command.execute());
    return new DefaultIBResource(pdtd, new Checksum(pdtd), of(LIQUIBASE_SCHEMA));

  }

  @Override
  public Optional<Map<String, IBResource>> schemaFrom(URLAndCreds in, String query, String nameSpace, String name,
      Optional<String> desc) {
    if (respondsTo(requireNonNull(in))) {
      Optional<Supplier<DataSource>> s = getDataSourceSupplier(in);
      return s.flatMap(dss -> {
        DataSource ds = dss.get();
        IBDatabaseDialect d = getDialect(in).orElseThrow(() -> new IBDataException("Unknown dialect " + in));
        SQLDialect dialect = SQLDialect.valueOf(d.getJooqName());
        Map<String, IBResource> k;
        try (Connection conn = ds.getConnection(); DSLContext create = DSL.using(ds.getConnection(), dialect);) {
          k = new HashMap<>();
          Path workingPath = getWorkingPathSupplier().get();
          k.put(DBUNIT_DTD, generateDbunitDTDSchema(conn, workingPath));
          final Result<Record> firstResult = create.fetch(query);
          PersistedIBSchema daSche = (PersistedIBSchema) ibSchemaFromRecordResults(getLog(), nameSpace, name,
              desc.orElse(null), in.getUrl(), firstResult, IbdataDatabaseTypeImplsVersioning.apiVersion());
          PersistedIBSchemaXpp3Writer writer = new PersistedIBSchemaXpp3Writer();
          Path p = workingPath.resolve(UUID.randomUUID() + XML);
          try (Writer ww = Files.newBufferedWriter(p)) {
            writer.write(ww, daSche);
            k.put(DEFAULT, new DefaultIBResource(p, new Checksum(p), of(IBSCHEMA_MIME_TYPE)));
          }
        } catch (DataAccessException | SQLException | IOException e) {
          getLog().error(FAILED_TO_QUERY_SCHEMA_FOR + in.getUrl());
          // Return empty after closing DSLContext
          k = null;
        }
        return ofNullable(k);
      });
    }
    return empty();
  }

  private final class DefaultIBDatabaseDialect implements IBDatabaseDialect {

    private final SQLDialect jooqDialect;
    private final String liquibase;
    private final Optional<ThirdParty> thirdParty;
    private final Map<String, Object> dbUnitUpdates;

    public DefaultIBDatabaseDialect(SQLDialect d, String liquibaseDatabaseClass, Map<String, Object> dbunitUpdates) {
      this.jooqDialect = requireNonNull(d);
      this.thirdParty = ofNullable(jooqDialect.thirdParty());
      this.liquibase = requireNonNull(liquibaseDatabaseClass);
      this.dbUnitUpdates = new HashMap<>();
      this.dbUnitUpdates.putAll(requireNonNull(dbunitUpdates));
      // see http://dbunit.sourceforge.net/faq.html#streaming
      this.dbUnitUpdates.put(DatabaseConfig.PROPERTY_RESULTSET_TABLE_FACTORY, new ForwardOnlyResultSetTableFactory());
    }

    @Override
    public String getJooqName() {
      return this.jooqDialect.getName();
    }

    @Override
    public Optional<String> hibernateDialectClass() {
      return this.thirdParty.map(ThirdParty::hibernateDialect);
    }

    @Override
    public String liquibaseDatabaseClass() {
      return this.liquibase;
    }

    @Override
    public Optional<String> springDbName() {
      return this.thirdParty.map(ThirdParty::springDbName);
    }

    @Override
    public Map<String, Object> getDbUnitConfigurationUpdates() {
      return this.dbUnitUpdates;
    }

  }

}