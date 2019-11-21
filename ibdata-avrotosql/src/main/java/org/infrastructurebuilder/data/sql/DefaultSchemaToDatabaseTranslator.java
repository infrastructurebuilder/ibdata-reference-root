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
import static org.apache.avro.Schema.Type.*;
import static org.infrastructurebuilder.data.IBDataException.cet;
import static org.infrastructurebuilder.data.sql.IBDataAvroToLiquibaseUtils.getLBTypeFromAvroSchemaType;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.dag.DAG;
import org.infrastructurebuilder.util.dag.DAGBuilder;

public class DefaultSchemaToDatabaseTranslator implements SchemaToDatabaseTranslator {
  public static final String DEFAULT_DATE_FORMAT = "yyyyMMddHHmm";
  public static final String NAME = "name";
  public static final String CREATE_TABLE = "createTable";

  private Path wp;
  private Supplier<String> df;
  private AtomicInteger i = new AtomicInteger(0);
  private final AtomicReference<String> date = new AtomicReference<>();

  public DefaultSchemaToDatabaseTranslator(PathSupplier wps) {
    this.df = () -> String.format("%s-%03d", date.get(), i.addAndGet(1));
    this.wp = wps.get();
  }

  @Override
  public synchronized Path apply(Schema u) {
    if (!date.compareAndSet(null, new SimpleDateFormat(DEFAULT_DATE_FORMAT).format(new Date())))
      throw new IBDataException("Could not set id value properly");

    if (requireNonNull(u, "DefaultSchemaToDatabaseTranslator schema").getType() != RECORD)
      throw new IBDataException("The converted schema named " + u.getFullName() + " must be of type " + RECORD);

    Xpp3Dom changeLog = new ChangeLog().asXML();
    //    List<LocalChangeSet> cs = schemaAsChangeSet(u);
    //    cs.forEach(c -> changeLog.addChild(c.asXML()));
    return cet.withReturningTranslation(() -> IBUtils
        .writeString(this.wp.resolve("LB-" + UUID.randomUUID().toString() + ".xml"), changeLog.toString()));
  }

  //  private List<LocalChangeSet> schemaAsChangeSet(Schema u) {
  //    List<LocalChangeSet> l = new ArrayList<>();
  //    if (u.getType() == RECORD) {
  //      LocalChangeSet cs = new LocalChangeSet(u);
  //      l.addAll(cs.getMeAndChildren());
  //    }
  //    return l;
}

abstract class LocalChangeSet {

  private final Xpp3Dom dom;

  public LocalChangeSet(Xpp3Dom u) {

    this.dom = requireNonNull(u);

  }

  public LocalChangeSet addChildField(Xpp3Dom ct) {
    this.dom.addChild(ct);
    return this;
  }

  public LocalChangeSet setAttribute(String name, String value) {
    this.dom.setAttribute(name, value);
    return this;
  }

  public Xpp3Dom asXML() {
    return this.dom;
  }
}

class ChangeLog extends LocalChangeSet {
  private static final String LB_CHANGELOG_NS = "http://www.liquibase.org/xml/ns/dbchangelog";

  public ChangeLog() {
    super(new Xpp3Dom("databaseChangeLog"));
    this
        //
        .setAttribute("xmlns", LB_CHANGELOG_NS)
        //
        .setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
        //
        .setAttribute("xmlns:ext", "http://www.liquibase.org/xml/ns/dbchangelog-ext").setAttribute("xsi:schemaLocation",
            "http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.1.xsd "
                + "http://www.liquibase.org/xml/ns/dbchangelog-ext http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-ext.xsd");

  }
}

class CreateTableChangeSet extends LocalChangeSet {
  public static final String AUTHOR = "author";
  public static final String CHANGE_SET = "changeSet";
  public static final String IBDATA_FROM_AVRO = "ibdata-from-avro";
  public static final String ID = "id";

  public CreateTableChangeSet(Schema u, Supplier<String> df) {
    super(new Xpp3Dom(CHANGE_SET));
    this.setAttribute(AUTHOR, IBDATA_FROM_AVRO).setAttribute(ID, df.get());
    u.getFields().stream().map(IBDataAvroToLiquibaseUtils::addField).forEach(this::addChildField);
    // Now the hard parts
    //  Indexes
    //  Constraints
    //  Subrecord tables
  }

}
