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

import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_DIRECTORY;
import static org.infrastructurebuilder.data.IBDataConstants.INGESTION_TARGET;
import static org.infrastructurebuilder.data.IBDataConstants.TRANSFORMATION_TARGET;
import static org.infrastructurebuilder.data.IBMetadataUtils.*;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.files.BasicIBChecksumPathType;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

public class InjectIBData implements ParameterResolver {

  public final static Logger log = LoggerFactory.getLogger(InjectIBData.class);
  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(IBDataSet.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    // FIXME Dunno how else to get WP other than set a system property and write a marker file
    Path workingPath = Paths
        .get(
            Optional.ofNullable(System.getProperties().getProperty(IBDATA_WORKING_DIRECTORY)).orElse("./target/ibdata"))
        .toAbsolutePath();
    Path marker = getMarkerFile(workingPath)
        .orElseThrow(() -> new IBDataException("No marker file found for " + workingPath.toString()));

    IBChecksumPathType v;
    try {
      v = new BasicIBChecksumPathType(new JSONObject(IBUtils.readFile(marker)));
    } catch (JSONException | IOException e) {
      throw new IBDataException("Marker file " + marker.toString() + " invalid", e);
    }
    URL u = IBDataException.cet.withReturningTranslation(() -> v.getPath().resolve(IBDATA).resolve(IBDATASET_XML).toUri().toURL());
    log.info(() -> "About to read " + u.toExternalForm());

    return IBDataTypeImplsModelUtils.mapDataSetToDefaultIBDataSet.apply(u)
        .orElseThrow(() -> new IBDataException("No ingestion or transformation available for " + marker.toString()));
  }

  private Optional<Path> getMarkerFile(Path workingPath) {
    LinkOption[] options = { LinkOption.NOFOLLOW_LINKS };
    Path marker = workingPath.resolve(INGESTION_TARGET + ".json");
    if (!Files.isRegularFile(marker, options))
      marker = workingPath.resolve(TRANSFORMATION_TARGET + ".json");
    if (!Files.isRegularFile(marker, options))
      marker = null;
    return Optional.ofNullable(marker);
  }

}
