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
package org.infrastructurebuilder.data.ingest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericRecord;
import org.infrastructurebuilder.IBConstants;
import org.infrastructurebuilder.data.Formatters;
import org.infrastructurebuilder.data.IBDataAvroUtils;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.BasicIBChecksumPathType;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.jooq.Record;
import org.slf4j.Logger;

public class JooqRecordWriter implements IBDataAvroRecordWriter<Record> {

  private final Path workingPath;
  private final Schema schema;
  private final Logger log;
  private final Formatters f;

  public JooqRecordWriter(LoggerSupplier l, PathSupplier wps, Schema schema, Formatters f) {
    this.log = Objects.requireNonNull(l).get();
    this.workingPath = Objects.requireNonNull(wps).get();
    this.schema = Objects.requireNonNull(schema);
    this.f = Objects.requireNonNull(f);
  }

  @Override
  public IBChecksumPathType writeRecords(Iterable<Record> result) {
    Path path = IBDataException.cet
        .withReturningTranslation(() -> Files.createTempFile(workingPath, "JooqRecords", ".avro"));
    try (DataFileWriter<GenericRecord> w = IBDataAvroUtils.fromSchemaAndPath.apply(path, schema)) {
      for (Record r : result) {
        w.append(new JooqRecordMapProxy(r).asGenericRecord(log, f, schema));
      }
    } catch (IOException e) {
      throw new IBDataException("Failed to writeRecords", e);
    }
    Checksum c = new Checksum(path);
    Path targetName = this.workingPath.resolve(c.asUUID().get().toString() + ".avro");
    return IBDataException.cet.withReturningTranslation(
        () -> new BasicIBChecksumPathType(path, c, IBConstants.AVRO_BINARY).moveTo(targetName));
  }
}
