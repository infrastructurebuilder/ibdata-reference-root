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

import static java.lang.String.format;
import static java.nio.file.Files.createTempFile;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.infrastructurebuilder.IBConstants.AVRO_BINARY;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;
import static org.infrastructurebuilder.data.IBDataException.cet;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericRecord;
import org.infrastructurebuilder.data.JooqAvroRecordWriterSupplier.JooqRecordWriter;
import org.infrastructurebuilder.data.ingest.IBDataAvroRecordWriter;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.AbstractConfigurableSupplier;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.BasicIBChecksumPathType;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.jooq.Record;
import org.slf4j.Logger;

@Named(JooqAvroRecordWriterSupplier.NAME)
public class JooqAvroRecordWriterSupplier extends AbstractConfigurableSupplier<JooqRecordWriter, ConfigMap, Object> {
  static final String NAME = "jooq-record-writer";
  public final static String SCHEMA = "schema";
  public final static String TARGET = "target";
  private final IBDataAvroUtilsSupplier aus;
  private final Schema schema;

  @Inject
  public JooqAvroRecordWriterSupplier(@Named(IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps, LoggerSupplier l,
      IBDataAvroUtilsSupplier aus) {
    this(wps, null, l, aus, null);
  }

  private JooqAvroRecordWriterSupplier(PathSupplier wps, ConfigMap config, LoggerSupplier l,
      IBDataAvroUtilsSupplier aus, Schema targetSchema) {
    super(wps, config, l);
    this.aus = Objects.requireNonNull(aus);
    this.schema = targetSchema;
  }

  @Override
  public JooqAvroRecordWriterSupplier configure(ConfigMap config) {
    IBDataAvroUtilsSupplier a = (IBDataAvroUtilsSupplier) this.aus.configure(config);
    return new JooqAvroRecordWriterSupplier(
        // My working path supplier
        getWps()
        // The config
        , config
        // Logger supplier
        , () -> getLog()
        // Avro utils supplier
        , a
        // The schema (or die)
        , ofNullable(
            // Schema string
            config.getString(SCHEMA))
                // Map to a schema
                .map(ss -> a.get().avroSchemaFromString(ss))
                // or die
                .orElseThrow(
                    () -> new IBDataException(format("No Schema available in % instance", this.getClass().getName()))));
  }

  @Override
  protected JooqRecordWriter getInstance(PathSupplier workingPath, Optional<Object> in) {
    return new JooqRecordWriter(getLog(), workingPath.get(), this.schema, this.aus.get());
  }

  /**
   * Writes Jooq Record instances to a stream as Avro GenericRecord instances
   * using the mapped Schema and optional GenericData
   *
   * @author mykel.alvis
   *
   */
  public class JooqRecordWriter implements IBDataAvroRecordWriter<Record> {

    private final Path workingPath;
    private final Schema schema;
    private final Logger log;
    private final IBDataAvroUtils f;

    public JooqRecordWriter(Logger l, Path wp, Schema schema, IBDataAvroUtils f) {
      this.log = requireNonNull(l);
      this.workingPath = requireNonNull(wp);
      this.schema = requireNonNull(schema);
      this.f = requireNonNull(f);
    }

    @Override
    public IBChecksumPathType writeRecords(Iterable<Record> result) {
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
      return cet.withReturningTranslation(() -> new BasicIBChecksumPathType(path, c, AVRO_BINARY).moveTo(targetName));
    }

  }

}
