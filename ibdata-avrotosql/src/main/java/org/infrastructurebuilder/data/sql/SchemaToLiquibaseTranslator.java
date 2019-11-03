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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Optional;
import java.util.function.BiFunction;

import org.apache.avro.Schema;
import org.infrastructurebuilder.IBException;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;

import liquibase.Liquibase;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.DatabaseConnection;
import liquibase.exception.ChangeLogParseException;
import liquibase.exception.LiquibaseException;
import liquibase.parser.core.xml.XMLChangeLogSAXParser;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;

public interface SchemaToLiquibaseTranslator extends BiFunction<Optional<Path>, Schema, Path> {
  public static ResourceAccessor createResourceOpener() {
    return new ClassLoaderResourceAccessor(SchemaToLiquibaseTranslator.class.getClassLoader());

    //        return new SpringResourceOpener(getChangeLog());
  }

  public static  Liquibase createLiquibase(Connection c, String changelog, ConfigMapSupplier cms, boolean dropFirst) throws LiquibaseException {
    ResourceAccessor resourceAccessor = createResourceOpener();
    Liquibase liquibase = new Liquibase(changelog, resourceAccessor, createDatabase(c, resourceAccessor));
    liquibase.setIgnoreClasspathPrefix(true);
    ConfigMap parameters = cms.get();
    if (parameters != null) {
      for (String entry : parameters.keySet()) {
        liquibase.setChangeLogParameter(entry, parameters.getString(entry));
      }
    }

    if (dropFirst) {
      liquibase.dropAll();
    }

    return liquibase;
  }


  public static DatabaseConnection createDatabase(Connection c, ResourceAccessor resourceAccessor) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  default Path apply(Optional<Path> l, Schema t) {
    Path retVal;
    try {
      retVal = Files.createTempFile("liquibase-", ".xml");
    } catch (IOException e1) {
      throw new IBException(e1);
      // FIXME Which exceptions should I actually deal with?
    }
    DatabaseChangeLog cl = l.map(m -> {
      XMLChangeLogSAXParser x = new XMLChangeLogSAXParser();
      ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor(getClass().getClassLoader());
      ChangeLogParameters changeLogParameters = new ChangeLogParameters();
      try {
        return x.parse(m.toAbsolutePath().toString(), changeLogParameters, resourceAccessor);
      } catch (ChangeLogParseException e) {
        throw new IBException(e);
      }
    }).orElse(new DatabaseChangeLog());

//    ChangeLogSerializer changeLogSerializer = ChangeLogSerializerFactory.getInstance().getSerializer("xml");
//    Document d = changeLogSerializer.
//        liquibase.generateChangeLog(catalogAndSchema, diffToChangeLog, printStream, changeLogSerializer);



    return retVal;
  }
}

//<databaseChangeLog
//    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
//    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
//    xmlns:ext="http://www.liquibase.org/xml/ns/dbchangelog-ext"
//    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.1.xsd
//    http://www.liquibase.org/xml/ns/dbchangelog-ext http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-ext.xsd">
//</databaseChangeLog>
