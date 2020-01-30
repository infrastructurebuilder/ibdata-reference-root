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

import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.config.AbstractCMSConfigurableSupplier;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.TableRecord;
import org.jooq.impl.CustomRecord;
import org.slf4j.Logger;

@Named
public class DefaultIBDataJooqUtilsSupplier extends AbstractCMSConfigurableSupplier<IBDataJooqUtils, Object> {
  public static final String JOOQ_RESULT_REPRESENTATIONAL_VALUE = "org.jooq.Result<Record>";

  @Inject
  public DefaultIBDataJooqUtilsSupplier(PathSupplier wps, ConfigMapSupplier config, LoggerSupplier l) {
    super(wps, config, l, null);
  }

  private DefaultIBDataJooqUtilsSupplier(PathSupplier wps, ConfigMapSupplier config, LoggerSupplier l, Object param) {
    super(wps, config, l, param);
  }

  @Override
  public DefaultIBDataJooqUtilsSupplier getConfiguredSupplier(ConfigMapSupplier cms) {
    Object param = null;
    return new DefaultIBDataJooqUtilsSupplier(getWorkingPathSupplier(), cms, () -> getLog(), param);
  }

  @Override
  protected IBDataJooqUtils getInstance(PathSupplier wps, Object in) {
    return new DefaultIBDataJooqUtils(getLog(), wps.get(), in);
  }

  private class DefaultIBDataJooqUtils implements IBDataJooqUtils {

    private final Logger log;
    private final Path workingPath;
    private final Object in;

    public DefaultIBDataJooqUtils(Logger log, Path workingPath, Object in) {
      this.log = log;
      this.workingPath = workingPath;
      this.in = in;
    }

    @Override
    public Logger getLog() {
      return log;
    }

    @Override
    public Optional<String> getInboundType() {
      return of(JOOQ_RESULT_REPRESENTATIONAL_VALUE);
    }

    @Override
    public Optional<String> getOutboundType() {
      return getInboundType();
    }

    @Override
    public Optional<List<Xpp3Dom>> to(List<IBSchema> s) {
      if (requireNonNull(s, "Inbound list of IBSchema").size() == 0)
        return of(Collections.emptyList());


      /*
       * Liquibase changelog generation
       */



      List<Record> list = new ArrayList<>();
      // Second and subsequent schema are considered subordinate
      List<IBSchema> others = s.size() > 1 ? s.subList(1, s.size()) : Collections.emptyList();
      // Each (IBSchema) instance becomes some form of record.
      Map<String, org.apache.avro.Schema.Field> fields = new HashMap<>();
      // Initially, just the first IBSchema is relevant
      IBSchema r = s.get(0);

      SortedSet<IBIndex> indexes = r.getSchemaIndexes();

      StringBuilder doc = new StringBuilder(
          r.getDescription().orElseThrow(() -> new IBDataException("Description cannot be null")))
              // UUID
              .append(" UUID:").append(r.getUuid().toString())
              // Mime type
              .append(" Content-Type:").append(r.getMimeType())
              // Source
              .append(" Original-URL:").append(r.getUrl().orElse("Unknown"))
              // Creation date
              .append(" Creation-Date: ").append(r.getCreationDate().toGMTString());
      String namespace = r.getNameSpace().orElseThrow(() -> new IBDataException("Namespace cannot be null"));
      String name = r.getName().orElseThrow(() -> new IBDataException("Name cannot be null"));

      // TODO What to do with r.getMetadata()?
      List<Field> l = r.getSchemaFields().parallelStream().filter(sd -> !sd.isDeprecated())
          .map(IBDataJooqUtils::toJooqField).collect(toList());

      Table<? extends TableRecord> t;
      CustomRecord<?> cr = null;

      list.add(cr);
//
//          Schema.createRecord(name, doc.toString(), namespace, false, l)
//          );

      if (others.size() > 0) {
        // TODO Process "others"
        getLog().warn("There were " + others.size() + " elements not processed at this time");
      }
      return Optional.empty();
//      return of(list);  // FIXME Get the correct to() for databases
    }

    @Override
    public Optional<List<IBSchema>> from(List<IBDataDecoratedDAO<Result<Record>>> s) {

      List<IBSchema> list = new ArrayList<>();
      if (requireNonNull(s, "Inbound list of records").size() > 1)
        throw new IBDataException("Inbound list must be at last size 1");
      IBDataDecoratedDAO<Result<Record>> itemP = s.get(0);
      Result<Record> results = itemP.get();
      IBSchema p = IBDataJooqUtils.ibSchemaFromRecordResults(log, itemP.getNameSpace(), itemP.getName(),
          itemP.getDescription(), itemP.getSource().orElse(null), results, itemP.getThisVersion());
      list.add(p);
      if (s.size() > 1)
        log.warn("Additional results were ignored at this time");
      return of(list);
    }
  }
}
