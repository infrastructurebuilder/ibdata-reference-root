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

import static java.nio.file.Files.createTempFile;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static org.infrastructurebuilder.IBConstants.AVRO_BINARY;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;
import static org.infrastructurebuilder.data.IBDataException.cet;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericRecord;
import org.infrastructurebuilder.data.ingest.IBDataRecordWriter;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.AbstractCMSConfigurableSupplier;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.DefaultIBResource;
import org.infrastructurebuilder.util.files.IBResource;
import org.jooq.Record;
import org.slf4j.Logger;

@Named(JooqAvroRecordWriterSupplier.NAME)
public class JooqAvroRecordWriterSupplier extends AbstractCMSConfigurableSupplier<IBDataRecordWriter<Record>, Schema> {
  static final String NAME = "jooq-record-writer";
  public final static String SCHEMA = "schema";
  public final static String TARGET = "target";
  private final IBDataAvroUtilsSupplier aus;

  @Inject
  public JooqAvroRecordWriterSupplier(@Named(IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps, LoggerSupplier l,
      IBDataAvroUtilsSupplier aus) {
    super(wps, null, l);
    this.aus = requireNonNull(aus);
  }

  private JooqAvroRecordWriterSupplier(PathSupplier wps, ConfigMapSupplier config, LoggerSupplier l,
      IBDataAvroUtilsSupplier aus) {
    super(wps, config, l, (Schema) config.get().get(SCHEMA));
    this.aus = (IBDataAvroUtilsSupplier) requireNonNull(aus).configure(config);
  }

  @Override
  public JooqAvroRecordWriterSupplier getConfiguredSupplier(ConfigMapSupplier cms) {
    return new JooqAvroRecordWriterSupplier(
        // My working path supplier
        getWorkingPathSupplier()
        // The config
        , cms
        // Logger supplier
        , () -> getLog()
        // Avro utils supplier
        , this.aus);
  }

  @Override
  protected JooqRecordWriter getInstance(PathSupplier wps, Schema in) {
    return new JooqRecordWriter(getLog(), wps.get(), () -> in, this.aus.get());
  }

  /**
   * Writes Jooq Record instances to a stream as Avro GenericRecord instances
   * using the mapped Schema and optional GenericData
   *
   * @author mykel.alvis
   *
   */
  public class JooqRecordWriter implements IBDataRecordWriter<Record> {

    private final Path workingPath;
    private final Schema schema;
    private final Logger log;
    private final IBDataAvroUtils f;

    public JooqRecordWriter(Logger l, Path wp, Supplier<Schema> schema, IBDataAvroUtils f) {
      this.log = requireNonNull(l);
      this.workingPath = requireNonNull(wp);
      this.schema = requireNonNull(schema).get();
      this.f = requireNonNull(f);
    }

    @Override
    public IBResource writeRecords(Iterable<Record> result) {
      // GenericData gd = new MapProxyGenericData(this.f);
      Path path = cet.withReturningTranslation(() -> createTempFile(workingPath, "JooqRecords", ".avro"));
      try (DataFileWriter<GenericRecord> w = f.fromSchemaAndPathAndTranslator(path, schema)) {
        for (Record r : result) {
          w.append(new JooqRecordMapProxy(r, schema, log).get());
        }
      } catch (IOException e) {
        throw new IBDataException("Failed to writeRecords", e);
      }
      Checksum c = new Checksum(path);
      Path targetName = this.workingPath.resolve(c.asUUID().get().toString() + ".avro");
      return cet.withReturningTranslation(() -> new DefaultIBResource(path, c, of(AVRO_BINARY)).moveTo(targetName));
    }

  }

}
