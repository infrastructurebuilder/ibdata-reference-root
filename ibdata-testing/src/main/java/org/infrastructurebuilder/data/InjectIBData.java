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

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATASET_XML;
import static org.infrastructurebuilder.data.IBDataConstants.MARKER_FILE;
import static org.infrastructurebuilder.data.IBDataException.cet;
import static org.infrastructurebuilder.data.IBDataTypeImplsModelUtils.mapDataSetToDefaultIBDataSet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.infrastructurebuilder.util.files.IBResource;
import org.infrastructurebuilder.util.files.model.IBCPTInputSource;
import org.infrastructurebuilder.util.files.model.io.xpp3.IBResourceModelXpp3ReaderEx;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

public class InjectIBData implements ParameterResolver {
  private final static Logger log = LoggerFactory.getLogger(InjectIBData.class);
  private final static TestingPathSupplier wps = new TestingPathSupplier();
  private final static IBResourceModelXpp3ReaderEx reader = new IBResourceModelXpp3ReaderEx();
  public final static Function<Path, ? extends IBResource> readIBCPT = (marker) -> {
    try (InputStream in = Files.newInputStream(marker, NOFOLLOW_LINKS)) {
      return cet.withReturningTranslation(() -> reader.read(in, true, new IBCPTInputSource()));
    } catch (IOException e) {
      throw new IBDataException("Marker file " + marker.toString() + " invalid", e);
    }

  };

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(IBDataSet.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    // FIXME how else to get WP other than set system property+write marker file
    IBResource v = readIBCPT.apply(wps.getRoot().resolve(MARKER_FILE).toAbsolutePath());
    URL u = cet.withReturningTranslation(() -> v.getPath().resolve(IBDATA).resolve(IBDATASET_XML).toUri().toURL());
    log.debug(() -> "About to read " + u.toExternalForm());
    return mapDataSetToDefaultIBDataSet.apply(u)
        .orElseThrow(() -> new IBDataException("No ingestion or transformation available for " + v.getPath()));
  }

}
