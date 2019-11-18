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

import static org.infrastructurebuilder.data.IBDataConstants.IBDATA;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATASET_XML;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_DIRECTORY;
import static org.infrastructurebuilder.data.IBDataConstants.INGESTION_TARGET;
import static org.infrastructurebuilder.data.IBDataConstants.TRANSFORMATION_TARGET;
import static org.infrastructurebuilder.data.IBDataException.cet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.infrastructurebuilder.data.model.DataSet;
import org.infrastructurebuilder.data.model.DataSetInputSource;
import org.infrastructurebuilder.data.model.io.xpp3.IBDataSourceModelXpp3ReaderEx;
import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.files.BasicIBChecksumPathType;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.infrastructurebuilder.util.files.model.IBCPTInputSource;
import org.infrastructurebuilder.util.files.model.io.xpp3.IBChecksumPathTypeModelXpp3ReaderEx;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

public class InjectIBData implements ParameterResolver {
  public final static Function<? super InputStream, ? extends IBChecksumPathType> readIBCPT = (in) -> {
    IBChecksumPathTypeModelXpp3ReaderEx reader;
    IBCPTInputSource dsis;

    reader = new IBChecksumPathTypeModelXpp3ReaderEx();
    dsis = new IBCPTInputSource();
    return cet.withReturningTranslation(() -> reader.read(in, true, dsis));
  };

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
    IBChecksumPathType v = getMarkerFile();
    URL u = cet.withReturningTranslation(() -> v.getPath().resolve(IBDATA).resolve(IBDATASET_XML).toUri().toURL());
    log.info(() -> "About to read " + u.toExternalForm());

    return IBDataTypeImplsModelUtils.mapDataSetToDefaultIBDataSet.apply(u)
        .orElseThrow(() -> new IBDataException("No ingestion or transformation available for " + v.getPath()));
  }

  private IBChecksumPathType getMarkerFile() {
    Path marker = Paths
        .get(Optional.ofNullable(System.getProperties().getProperty("target_dir"))
            .orElseThrow(() -> new IBDataException("Cannot locate marker file")))
        .resolve(IBDataConstants.MARKER_FILE).toAbsolutePath();
    LinkOption[] options = { LinkOption.NOFOLLOW_LINKS };
    try (InputStream ins = Files.newInputStream(marker, options)) {
      IBChecksumPathType v;
      v = readIBCPT.apply(ins);
      return v;
    } catch (IOException e) {
      throw new IBDataException("Marker file " + marker.toString() + " invalid", e);
    }
  }

}
