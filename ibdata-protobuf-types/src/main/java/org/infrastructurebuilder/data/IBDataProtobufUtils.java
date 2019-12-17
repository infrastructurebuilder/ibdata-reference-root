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
import static org.infrastructurebuilder.data.IBDataException.cet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.xml.validation.Schema;

import org.infrastructurebuilder.data.transform.line.ProtobufDataFileWriter;
import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.config.ConfigMap;

import com.google.protobuf.GeneratedMessageV3;

public interface IBDataProtobufUtils {
  public static final String NO_SCHEMA_CONFIG_FOR_MAPPER = "No schema config for mapper";
  public final static String JAR_PREFIX = "jar:"; // TODO Move to IBConstants next core release

//  public static final Function<String, Schema> avroSchemaFromString = schema -> {
//    String q = ofNullable(schema).orElseThrow(() -> new IBDataException(NO_SCHEMA_CONFIG_FOR_MAPPER + "3"));
//    String s = IBDataException.cet.withReturningTranslation(
//        () -> ((Files.exists(Paths.get(schema))) ? Paths.get(schema).toUri().toURL().toExternalForm() : q));
//
//    boolean isURL = s.startsWith(JAR_PREFIX) || s.startsWith(HTTP_PREFIX) || s.startsWith(HTTPS_PREFIX)
//        || s.startsWith(FILE_PREFIX) || s.startsWith(ZIP_PREFIX);
//    try (InputStream in = isURL ? IBUtils.translateToWorkableArchiveURL(s).openStream()
//        : IBDataProtobufUtils.class.getResourceAsStream(s)) {
//      return cet.withReturningTranslation(() -> new Schema.Parser().parse(in));
//    } catch (IOException e) {
//      throw new IBDataException(e); // Handles the clos() of try-with-resources
//    }
//  };

  public final static BiFunction<Path, ConfigMap, ProtobufDataFileWriter> fromMapAndWP = (workingPath, map) -> {
    // Get the schema or die
    String schema = ofNullable(requireNonNull(map).getString("schema"))
        .orElseThrow(() -> new IBDataException("Schema is required"));
    Class<?> clazz;
    try {
      clazz = Class.forName(schema);
      GeneratedMessageV3 s = (GeneratedMessageV3) clazz.newInstance();
      return ProtobufDataFileWriter.create(workingPath);
    } catch (ClassNotFoundException e) {
      throw new IBDataException("Schema " + schema + " not available for instantiation");
    } catch (InstantiationException | IllegalAccessException | ClassCastException cce) {
      throw new IBDataException("Schema " + schema + " is not a GeneratedMessageV3");
    }
  };

  /**
   * Produces a DataFileWriter for the schema provided. If supplied, a GenericData
   * will be used for translation.
   *
   * @param targetPath  The file to write
   * @param s           The schema to use in the output file
   * @param genericData Optional GenericData. If not provided a default
   *                    GenericData instannnce will be used with no converters.
   * @return DataFileWriter to write records
   */
  public static ProtobufDataFileWriter fromSchemaAndPathAndTranslator(Path targetPath, GeneratedMessageV3 s,
      Optional<GeneratedMessageV3> genericData) {
    GeneratedMessageV3 gd = requireNonNull(genericData)
        .orElseThrow(() -> new IBDataException("Data required for fromSchemaAndPathAndTranslator"));
    // Get the DataFileWriter or die
    return ProtobufDataFileWriter.create(targetPath);
  }

}
