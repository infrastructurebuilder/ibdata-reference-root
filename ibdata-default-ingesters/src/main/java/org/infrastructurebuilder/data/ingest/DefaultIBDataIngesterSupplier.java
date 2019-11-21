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
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.infrastructurebuilder.data.IBDataConstants.*;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;
import static org.infrastructurebuilder.data.IBDataSource.SPLIT_ZIPS_CONFIG;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.data.DefaultIBDataStream;
import org.infrastructurebuilder.data.DefaultIBDataStreamIdentifier;
import org.infrastructurebuilder.data.DefaultIBDataStreamSupplier;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBDataIngester;
import org.infrastructurebuilder.data.IBDataSetIdentifier;
import org.infrastructurebuilder.data.IBDataSource;
import org.infrastructurebuilder.data.IBDataSourceSupplier;
import org.infrastructurebuilder.data.IBDataStream;
import org.infrastructurebuilder.data.IBMetadataUtils;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.DefaultConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.slf4j.Logger;

@Named(DefaultIBDataIngesterSupplier.NAME)
public class DefaultIBDataIngesterSupplier extends AbstractIBDataIngesterSupplier {

  public static final String NAME = "default";
  public static final String UNZIP = "unarchive";
  private final PathSupplier cds;

  @Inject
  public DefaultIBDataIngesterSupplier(@Named(IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps, LoggerSupplier log, @Named(IBDATA_DOWNLOAD_CACHE_DIR_SUPPLIER) PathSupplier cds) {
    this(wps, log, null, requireNonNull(cds));
  }

  private DefaultIBDataIngesterSupplier(PathSupplier wps, LoggerSupplier log, ConfigMapSupplier cms, PathSupplier cds) {
    super(wps, log, cms);
    this.cds = requireNonNull(cds);
  }

  @Override
  final protected IBDataIngester configuredType(ConfigMapSupplier config) {
    return new DefaultIBDataIngester(getWps().get(), getLog(), config.get(), cds.get());
  }

  @Override
  public DefaultIBDataIngesterSupplier getConfiguredSupplier(ConfigMapSupplier config) {
    return new DefaultIBDataIngesterSupplier(getWps(), () -> getLog(), config, this.cds);
  }

  final static DefaultIBDataStreamSupplier xyz(Path workingPath, IBDataSource source,
      IBChecksumPathType ibPathChecksumType, Date now) {
    Path localPath = ibPathChecksumType.getPath();
    String p = requireNonNull(workingPath).relativize(localPath).toString();
    Checksum c = ibPathChecksumType.getChecksum();
    UUID id = c.asUUID().get();

    DefaultIBDataStreamIdentifier ddsi = new DefaultIBDataStreamIdentifier(id
    // Created URL
        , Optional.of(source.getSourceURL())
        // Name
        , source.getName()
        //
        , source.getDescription()
        //
        , ibPathChecksumType.getChecksum()
        //
        , now
        //
        , source.getMetadata().orElse(IBMetadataUtils.emptyDocumentSupplier.get())
        //
        , ibPathChecksumType.getType()
        //
        , Optional.of(p));

    return new DefaultIBDataStreamSupplier(new DefaultIBDataStream(ddsi, ibPathChecksumType));

  };

  final static Stream<Supplier<IBDataStream>> dataSourceToStreamMapper(Path workingPath, Date now,
      IBDataSource source) {
    List<IBChecksumPathType> list = source.get();
    return list.stream().map(ibPathChecksumType -> xyz(workingPath, source, ibPathChecksumType, now));
  };

  public final class DefaultIBDataIngester extends AbstractIBDataIngester {

    private final Path cacheDirectory;
    private final boolean splitZips;

    public DefaultIBDataIngester(Path workingPath, Logger log, ConfigMap config, Path cacheDirectory) {
      super(workingPath, log, config);
      this.cacheDirectory = requireNonNull(cacheDirectory);
      //      ofNullable(
      //          requireNonNull(config, "Config map not supplied").getString(CACHE_DIRECTORY_CONFIG_ITEM)).map(Paths::get)
      //              .orElseThrow(() -> new IBDataException("No cache directory specified"));
      this.splitZips = ofNullable(
          requireNonNull(config, "Config map not supplied").getString(SPLIT_ZIPS_CONFIG))
              .map(Boolean::parseBoolean).orElse(false);
    }

    @Override
    public List<Supplier<IBDataStream>> ingest(Ingestion ingest, IBDataSetIdentifier dsi,
        SortedMap<String, IBDataSourceSupplier> dssList) {
      requireNonNull(dsi, "IBDataSetIdentifier for ingestion");
      requireNonNull(dssList, "List of IBDataSourceSupplier instances");
      Date now = new Date(); // Ok for "now"  (Get it?)
      ConfigMap over = new ConfigMap();
      over.put(IBDataSource.TARGET_PATH, getWorkingPath());
      //      over.put(IBDataSource.CACHE_DIR, this.cacheDirectory);
      over.put(UNZIP, this.splitZips);
      ConfigMap cms = new DefaultConfigMapSupplier(getConfig()).overrideConfiguration(over).get();
      return dssList.values().stream().map(Supplier::get).map(ds -> ds.withAdditionalConfig(cms))
          .flatMap(u -> dataSourceToStreamMapper(getWorkingPath(), now, u)).collect(toList());
    }

  }

}
