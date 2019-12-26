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

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.infrastructurebuilder.IBConstants.AVRO_BINARY;
import static org.infrastructurebuilder.data.IBDataException.cet;
import static org.infrastructurebuilder.util.IBUtils.copyToDeletedOnExitTempPath;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.avro.file.DataFileReader;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;

public class DefaultAvroGenericRecordStreamSupplier implements IBDataSpecificStreamFactory<GenericRecord> {
  public final static List<String> TYPES = Arrays.asList(AVRO_BINARY);

  public final static Function<InputStream, Optional<Stream<GenericRecord>>> genericStreamFromInputStream = (ins) -> {
    List<GenericRecord> l = new Vector<>();
    Path target = cet.withReturningTranslation(() -> copyToDeletedOnExitTempPath("ib", ".avro", ins));
    cet.withTranslation(() -> ins.close());
    DatumReader<GenericRecord> datumReader = new GenericDatumReader<GenericRecord>();
    try (DataFileReader<GenericRecord> dataFileReader = new DataFileReader<>(target.toFile(), datumReader)) {
      dataFileReader.forEach(l::add);
      return of(l.stream());
    } catch (IOException e) {
      return empty();
    } finally {
      cet.withTranslation(() -> {
        Files.delete(target);
      });
    }
  };

  @Override
  public Optional<Stream<GenericRecord>> from(IBDataStream ds) {
    return ofNullable(ds).flatMap(d -> genericStreamFromInputStream.apply(d.get()));
  }

  @Override
  public List<String> getRespondTypes() {
    return TYPES;
  }

}
