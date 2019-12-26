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

import java.nio.file.Path;
import java.util.Optional;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.slf4j.Logger;

public interface IBDataAvroUtils {
  public static final String NO_SCHEMA_CONFIG_FOR_MAPPER = "No schema config for mapper";
  public final static String JAR_PREFIX = "jar:"; // TODO Move to IBConstants next core release

  /**
   * Produces a DataFileWriter for the schema provided.  If supplied, a GenericData will be used for
   * translation.
   *
   * @param targetPath The file to write
   * @param s The schema to use in the output file
   * @param genericData Optional GenericData.  If not provided a default GenericData instannnce will be used with no converters.
   * @return DataFileWriter to write records
   */
  DataFileWriter<GenericRecord> fromSchemaAndPathAndTranslator(Path targetPath, Schema s);

  Schema avroSchemaFromString(String schema);

  DataFileWriter<GenericRecord> fromMapAndWP(Path workingPath, String schema);

  GenericData getGenericData();

  Logger getLog();

}
