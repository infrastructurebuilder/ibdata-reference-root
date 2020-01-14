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
import static java.util.Optional.ofNullable;
import static org.infrastructurebuilder.IBConstants.FILE_PREFIX;
import static org.infrastructurebuilder.IBConstants.HTTPS_PREFIX;
import static org.infrastructurebuilder.IBConstants.HTTP_PREFIX;
import static org.infrastructurebuilder.IBConstants.ZIP_PREFIX;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;
import static org.infrastructurebuilder.data.IBDataException.cet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.infrastructurebuilder.data.model.PersistedIBSchema;
import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.config.AbstractConfigurableSupplier;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.slf4j.Logger;

@Named
public class DefaultIBDataAvroUtilsSupplier extends AbstractConfigurableSupplier<IBDataAvroUtils, ConfigMap,Object>
    implements IBDataAvroUtilsSupplier {

  private final GenericDataSupplier gds;

  @Inject
  public DefaultIBDataAvroUtilsSupplier(@Named(IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps, LoggerSupplier ls, GenericDataSupplier gds) {
    this(wps, null, ls, gds);
  }

  private DefaultIBDataAvroUtilsSupplier(PathSupplier wps, ConfigMap config, LoggerSupplier l, GenericDataSupplier gds) {
    super(wps, config, l);
    this.gds = requireNonNull(gds);
  }

  @Override
  public IBDataAvroUtilsSupplier configure(ConfigMap config) {
    return new DefaultIBDataAvroUtilsSupplier(getWps(), config, () -> getLog(), (GenericDataSupplier) gds.configure(config));
  }

  @Override
  protected IBDataAvroUtils getInstance(PathSupplier wps, Optional<Object> in) {
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
      String s = IBDataException.cet.withReturningTranslation(
          () -> ((Files.exists(Paths.get(schema))) ? Paths.get(schema).toUri().toURL().toExternalForm() : q));

      boolean isURL = s.startsWith(JAR_PREFIX) || s.startsWith(HTTP_PREFIX) || s.startsWith(HTTPS_PREFIX)
          || s.startsWith(FILE_PREFIX) || s.startsWith(ZIP_PREFIX);
      try (InputStream in = isURL ? IBUtils.translateToWorkableArchiveURL(s).openStream()
          : IBDataAvroUtils.class.getResourceAsStream(s)) {
        return cet.withReturningTranslation(() -> new Schema.Parser().parse(in));
      } catch (IOException e) {
        throw new IBDataException(e); // Handles the clos() of try-with-resources
      }
    };

    @Override
    public final DataFileWriter<GenericRecord> fromMapAndWP(Path workingPath, String schema) {
      Schema s = avroSchemaFromString(schema);
      // Get the DataFileWriter or die
      DataFileWriter<GenericRecord> w = new DataFileWriter<GenericRecord>(
          new GenericDatumWriter<GenericRecord>(s, getGenericData(/*new Formatters(map)*/)));
      // create the working data file or die
      cet.withTranslation(() -> w.create(s, workingPath.toFile()));
      return w;
    };

    @Override
    public DataFileWriter<GenericRecord> fromSchemaAndPathAndTranslator(Path targetPath, Schema s) {
      // Get the DataFileWriter or die
      DataFileWriter<GenericRecord> w = new DataFileWriter<>(
          new GenericDatumWriter<>(requireNonNull(s), getGenericData()));
      // create the working data file or die
      cet.withTranslation(() -> w.create(s, targetPath.toFile()));
      return w;
    }

    @Override
    public final List<PersistedIBSchema> fromAvroSchema(Schema schema) {
      return Collections.emptyList();
    }
  }


}
