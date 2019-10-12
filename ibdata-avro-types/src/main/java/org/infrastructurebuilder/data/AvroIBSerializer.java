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
import static org.infrastructurebuilder.data.IBDataException.cet;

import java.nio.file.Path;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;

@Named("avro")
public class AvroIBSerializer extends AbstractIBSerializer<GenericRecord, Schema, DataFileWriter<GenericRecord>> {

  private final Optional<DataFileWriter<GenericRecord>> serializer;

  @Inject
  public AvroIBSerializer() {
    this(empty(), empty());
  }

  private AvroIBSerializer(Optional<Path> p, Optional<Schema> s) {
    super(p, s);
    if (getConfig().isPresent() && getPath().isPresent()) {
      DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<GenericRecord>(getConfig().get());
      serializer = of(new DataFileWriter<GenericRecord>(datumWriter));
    } else {
      serializer = empty();

    }
    serializer.ifPresent(dataFileWriter -> cet
        .withTranslation(() -> dataFileWriter.create(getConfig().get(), getPath().get().toFile())));
  }

  @Override
  public Optional<DataFileWriter<GenericRecord>> getSerializer() {
    return serializer;
  }

  @Override
  protected IBSerializer<GenericRecord, Schema, DataFileWriter<GenericRecord>> newInstance(Optional<Path> p,
      Optional<Schema> c) {
    return new AvroIBSerializer(p, c);
  }

}
