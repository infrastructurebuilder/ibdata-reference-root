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

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.infrastructurebuilder.IBConstants.AVRO_BINARY;
import static org.infrastructurebuilder.data.IBDataException.cet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;

public class DefaultAvroGenericRecordStreamSupplier implements IBDataSpecificStreamFactory {
  public final static List<String> TYPES = Arrays.asList(AVRO_BINARY);

  // FIXME Probably dangles an unclosed file
  @Override
  public Optional<Stream<Object>> from(IBDataStream ds) {
    return Optional.ofNullable(ds).map(d -> {
      try (InputStream ins = d.get()) {
      return stream(
          // From splterator
          spliteratorUnknownSize(
              // From DataFileStream
              cet.withReturningTranslation(
                  () -> new DataFileStream<GenericRecord>(ins, new GenericDatumReader<GenericRecord>())),
              // with no characteristics
              0),
          // not parallel
          false);
      } catch (IOException e) {
        throw new IBDataException(e);
      }
    });

  }

  @Override
  public List<String> getRespondTypes() {
    return TYPES;
  }

}
