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

import static org.infrastructurebuilder.IBConstants.APPLICATION_PDF;
import static org.infrastructurebuilder.IBConstants.APPLICATION_XML;
import static org.infrastructurebuilder.IBConstants.APPLICATION_ZIP;
import static org.infrastructurebuilder.IBConstants.AVRO_BINARY;
import static org.infrastructurebuilder.IBConstants.TEXT_CSV;
import static org.infrastructurebuilder.IBConstants.TEXT_PLAIN;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.util.files.TypeToExtensionMapper;


@Named
public class DefaultTypeToExtensionMapper implements TypeToExtensionMapper {

  @Inject
  public DefaultTypeToExtensionMapper() {
  }

  @Override
  public String getExtensionForType(String type) {
    switch (type) {
    case APPLICATION_XML:
      return ".xml";
    case TEXT_PLAIN:
      return ".txt";
    case APPLICATION_ZIP:
      return ".zip";
    case TEXT_CSV:
      return ".csv";
    case APPLICATION_PDF:
      return ".pdf";
    case "avro/binary":
      return AVRO_BINARY;
    default:
      return ".bin";
    }
  }

}
