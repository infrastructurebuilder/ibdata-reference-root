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
package org.infrastructurebuilder.data.ingest;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.logging.Log;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBDataIngester;
import org.infrastructurebuilder.data.IBDataIngesterSupplier;
import org.infrastructurebuilder.data.IBMetadataUtils;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;

@Named("default")
public class DefaultIBDataIngesterSupplier implements IBDataIngesterSupplier {

  private final PathSupplier wps;
  private final ConfigMapSupplier config;
  private final Log log;
  private final Path cacheDirectory;

  @Inject
  public DefaultIBDataIngesterSupplier(@Named(IBMetadataUtils.IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps,
      @Named(ConfigMapSupplier.MAVEN) ConfigMapSupplier cms, Log log) {
    this(wps, cms, log, null);
  }

  private DefaultIBDataIngesterSupplier(PathSupplier wps, ConfigMapSupplier cms, Log log, Path cacheDirectory) {
    this.wps = requireNonNull(wps);
    this.config = requireNonNull(cms);
    this.log = requireNonNull(log);
    this.cacheDirectory = cacheDirectory;
  }

  public Log getLog() {
    return log;
  }

  @Override
  public IBDataIngesterSupplier config(ConfigMapSupplier cms) {
    Optional<String> configItem = Optional.ofNullable(cms.get().get(IBMetadataUtils.CACHE_DIRECTORY_CONFIG_ITEM));
    Path cacheConfig = configItem.map(Paths::get)
        .orElseThrow(() -> new IBDataException("No cache directory specified"));
    return new DefaultIBDataIngesterSupplier(wps, cms, log, cacheConfig);
  }

  @Override
  public IBDataIngester get() {
    return new DefaultIBDataIngester(wps.get(), config.get(), log, cacheDirectory);
  }

}
