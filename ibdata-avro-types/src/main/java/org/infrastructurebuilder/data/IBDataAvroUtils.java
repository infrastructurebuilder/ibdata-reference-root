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

import static java.util.Optional.ofNullable;
import static org.infrastructurebuilder.IBConstants.FILE_PREFIX;
import static org.infrastructurebuilder.IBConstants.HTTPS_PREFIX;
import static org.infrastructurebuilder.IBConstants.HTTP_PREFIX;
import static org.infrastructurebuilder.IBConstants.ZIP_PREFIX;
import static org.infrastructurebuilder.data.IBDataException.cet;
import static org.infrastructurebuilder.data.transform.line.DefaultMapToGenericRecordIBDataLineTransformerSupplier.SCHEMA_PARAM;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.infrastructurebuilder.util.config.ConfigMap;

public interface IBDataAvroUtils {
  public static final String NO_SCHEMA_CONFIG_FOR_MAPPER = "No schema config for mapper";

  public static final Function<String, Schema> getSchema = schema -> {
    String s = ofNullable(schema).orElseThrow(() -> new IBDataException(NO_SCHEMA_CONFIG_FOR_MAPPER + "3"));
    try {
      s = (Files.exists(Paths.get(schema))) ? Paths.get(schema).toUri().toURL().toExternalForm() : s;
    } catch (MalformedURLException e1) {
      // Do nothing.  see if it works!
    }

    boolean isURL = s.startsWith(HTTP_PREFIX) || s.startsWith(HTTPS_PREFIX) || s.startsWith(FILE_PREFIX)
        || s.startsWith(ZIP_PREFIX);
    try (InputStream in = isURL ? new URL(s).openStream() : IBDataAvroUtils.class.getResourceAsStream(s)) {
      return cet.withReturningTranslation(() -> new Schema.Parser().parse(in));
    } catch (IOException e) {
      throw new IBDataException(e); // Handles the clos() of try-with-resources
    }
  };

  public final static BiFunction<Path, ConfigMap, DataFileWriter<GenericRecord>> fromMapAndWP = (workingPath, map) -> {
    // Get the schema or die
    Optional<String> schema = ofNullable(Objects.requireNonNull(map).getString(SCHEMA_PARAM));
    Schema s = getSchema.apply(schema.orElseThrow(() -> new IBDataException(NO_SCHEMA_CONFIG_FOR_MAPPER + " 2")));
    // Get the DataFileWriter or die
    DataFileWriter<GenericRecord> w = new DataFileWriter<GenericRecord>(new GenericDatumWriter<GenericRecord>(s));
    // create the working data file or die
    cet.withTranslation(() -> w.create(s, workingPath.toFile()));
    return w;
  };

}
