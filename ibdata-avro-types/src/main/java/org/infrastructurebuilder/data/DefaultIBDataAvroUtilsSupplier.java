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

import static java.nio.file.Files.exists;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.infrastructurebuilder.IBConstants.FILE_PREFIX;
import static org.infrastructurebuilder.IBConstants.HTTPS_PREFIX;
import static org.infrastructurebuilder.IBConstants.HTTP_PREFIX;
import static org.infrastructurebuilder.IBConstants.ZIP_PREFIX;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;
import static org.infrastructurebuilder.data.IBDataException.cet;
import static org.infrastructurebuilder.util.IBUtils.translateToWorkableArchiveURL;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.infrastructurebuilder.IBConstants;
import org.infrastructurebuilder.data.model.PersistedIBSchema;
import org.infrastructurebuilder.data.model.SchemaField;
import org.infrastructurebuilder.data.model.SchemaIndex;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.config.AbstractCMSConfigurableSupplier;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.slf4j.Logger;

@Named
public class DefaultIBDataAvroUtilsSupplier extends AbstractCMSConfigurableSupplier<IBDataAvroUtils, Object>
    implements IBDataAvroUtilsSupplier {

  private final GenericDataSupplier gds;

  @Inject
  public DefaultIBDataAvroUtilsSupplier(@Named(IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps, LoggerSupplier ls,
      GenericDataSupplier gds) {
    super(wps, null, ls);
    this.gds = requireNonNull(gds);
  }

  private DefaultIBDataAvroUtilsSupplier(PathSupplier wps, ConfigMapSupplier config, LoggerSupplier l,
      GenericDataSupplier gds) {
    super(wps, config, l);
    this.gds = (GenericDataSupplier) requireNonNull(gds).configure(config);
  }

  @Override
  public DefaultIBDataAvroUtilsSupplier getConfiguredSupplier(ConfigMapSupplier cms) {
    return new DefaultIBDataAvroUtilsSupplier(getWorkingPathSupplier(), cms, () -> getLog(), gds);
  }

  @Override
  protected IBDataAvroUtils getInstance(PathSupplier wps, Object in) {
    return new DefaultIBDataAvroUtils(getLog(), gds.get());
  }

  public class DefaultIBDataAvroUtils implements IBDataAvroUtils {

    private final GenericData gd;
    private final Logger logger;

    public DefaultIBDataAvroUtils(Logger ls, GenericData gds) {
      this.logger = ls;
      this.gd = gds;
    }

    @Override
    public GenericData getGenericData() {
      return this.gd;
    }

    @Override
    public Logger getLog() {
      return this.logger;
    }

    @Override
    public final Schema avroSchemaFromString(String schema) {
      String q = ofNullable(schema).orElseThrow(() -> new IBDataException(NO_SCHEMA_CONFIG_FOR_MAPPER + "3"));
      String s = cet.withReturningTranslation(
          () -> ((exists(Paths.get(schema))) ? Paths.get(schema).toUri().toURL().toExternalForm() : q));

      boolean isURL = s.startsWith(JAR_PREFIX) || s.startsWith(HTTP_PREFIX) || s.startsWith(HTTPS_PREFIX)
          || s.startsWith(FILE_PREFIX) || s.startsWith(ZIP_PREFIX);
      try (InputStream in = isURL ? translateToWorkableArchiveURL(s).openStream() : getClass().getResourceAsStream(s)) {
        return cet.withReturningTranslation(() -> new Schema.Parser().parse(in));
      } catch (IOException e) {
        throw new IBDataException(e);
      }
    };

    @Override
    public final DataFileWriter<GenericRecord> getGenericRecordWriterFrom(Path workingPath, String schema) {
      Schema s = avroSchemaFromString(schema);
      // Get the DataFileWriter or die
      DataFileWriter<GenericRecord> w = new DataFileWriter<GenericRecord>(
          new GenericDatumWriter<GenericRecord>(s, getGenericData(/* new Formatters(map) */)));
      // create the working data file or die
      cet.withTranslation(() -> w.create(s, workingPath.toFile()));
      return w;
    };

    @Override
    public DataFileWriter<GenericRecord> fromSchemaAndPathAndTranslator(Path targetPath, Schema s) {
      // Get the DataFileWriter or die
      DataFileWriter<GenericRecord> w = new DataFileWriter<>(
          new GenericDatumWriter<>(requireNonNull(s, "Inbound avro schema"), getGenericData()));
      // create the working data file or die
      cet.withTranslation(() -> w.create(s, targetPath.toFile()));
      return w;
    }

    @Override
    public Optional<List<IBSchema>> from(List<IBDataDecoratedDAO<Schema>> s) {
      List<IBSchema> list = new ArrayList<>();
      if (requireNonNull(s, "Inbound list of avro schemas").size() > 1)
        throw new IBDataException("Inbound list must be at least size 1");
      IBDataDecoratedDAO<Schema> dao = s.get(0);
      Schema schema = dao.get();
      PersistedIBSchema p = new PersistedIBSchema();
      p.setCreationDate(new Date());
      p.setDescription(schema.getDoc());
      p.setName(schema.getName());
      p.setMimeType(IBConstants.IBDATA_SCHEMA);
      Metadata metadata = new Metadata();
      List<SchemaField> fields = schema.getFields().stream().map(IBDataAvroUtils::toIBSchemaField).collect(toList());
      List<SchemaIndex> indexes = new ArrayList<>();
      for (int i = 0; i < fields.size(); ++i) {
         //  TODO       fields.get(i).setVersionAppeared(versionAppeared);
        fields.get(i).setIndex(i);
      }
      p.setMetadata(metadata);
      p.setFields(fields);
      p.setIndexes(indexes);
      list.add(p);
      return of(list);
    }

    @Override
    public Optional<List<Schema>> to(List<IBSchema> s) {
      if (requireNonNull(s, "Inbound list of IBSchema").size() == 0)
        return of(Collections.emptyList());
      List<Schema> list = new ArrayList<>();
      // Second and subsequent schema are considered subordinate
      List<IBSchema> others = s.size() > 1 ? s.subList(1, s.size()) : Collections.emptyList();
      // Each (IBSchema) instance becomes some form of record.
      Map<String, org.apache.avro.Schema.Field> fields = new HashMap<>();
      // Initially, just the first IBSchema is relevant
      IBSchema r = s.get(0);

      SortedSet<IBIndex> indexes = r.getSchemaIndexes();

      StringBuilder doc = new StringBuilder(r.getDescription().orElseThrow(() -> new IBDataException("Description cannot be null")))
          // UUID
          .append("\n| UUID: ").append(r.getUuid().toString())
          // Mime type
          .append("\n| Content-Type: ").append(r.getMimeType())
          // Source
          .append("\n| Original-URL: ").append(r.getUrl().orElse("Unknown"))
          // Creation date
          .append("\n| Creation-Date: ").append(r.getCreationDate().toGMTString());
      String namespace = r.getNameSpace().orElseThrow(() -> new IBDataException("Namespace cannot be null"));
      String name = r.getName().orElseThrow(() -> new IBDataException("Name cannot be null"));

      // TODO What to do with r.getMetadata()?
      List<Field> l = r.getSchemaFields().parallelStream().filter(sd -> !sd.isDeprecated())
          .map(IBDataAvroUtils::toAvroField).collect(toList());

      list.add(Schema.createRecord(name, doc.toString(), namespace, false, l));

      if (others.size() > 0) {
        // TODO Process "others"
        getLog().warn("There were " + others.size() + " elements not processed at this time");
      }
      return of(list);
    }

    @Override
    public Optional<String> getInboundType() {
      return of(Schema.class.getName());
    }

    @Override
    public Optional<String> getOutboundType() {
      return getInboundType();
    }

  }

}
