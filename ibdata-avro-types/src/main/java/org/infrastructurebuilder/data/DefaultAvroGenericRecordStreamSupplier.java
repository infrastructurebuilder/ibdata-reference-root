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
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Stream;

import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;

public class DefaultAvroGenericRecordStreamSupplier implements IBDataSpecificStreamFactory {
  public final static List<String> TYPES = Arrays.asList(AVRO_BINARY);

  @Override
  public Optional<Stream<? extends Object>> from(IBDataStream ds) {
    return Optional.ofNullable(ds).map(d -> {
      // FIXME This stream close prematurely

      final InputStream ins = d.get();
      DataFileStream<GenericRecord> s = cet.withReturningTranslation(() -> {
        return new DataFileStream<GenericRecord>(ins, new GenericDatumReader<GenericRecord>());
      });
      try {
        // FIXME OBVIOUSLY NOT CORRECT!!!!
        List<GenericRecord> l = new Vector<>();
        s.forEach(a -> l.add(a));
        return l.stream();
//        stream(
//            // From splterator
//            spliteratorUnknownSize(s, 0), // with no characteristics
//            // not parallel
//            false);
      } finally {
        if (s != null)
          try {
            s.close();
            ins.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
      }
    });

  }

  @Override
  public List<String> getRespondTypes() {
    return TYPES;
  }

}
