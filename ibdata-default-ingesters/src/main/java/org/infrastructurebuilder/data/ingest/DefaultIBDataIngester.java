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
import static java.util.stream.Collectors.toList;

import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import org.infrastructurebuilder.data.DefaultIBDataStream;
import org.infrastructurebuilder.data.DefaultIBDataStreamIdentifier;
import org.infrastructurebuilder.data.DefaultIBDataStreamSupplier;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBDataIngester;
import org.infrastructurebuilder.data.IBDataSetIdentifier;
import org.infrastructurebuilder.data.IBDataSource;
import org.infrastructurebuilder.data.IBDataSourceSupplier;
import org.infrastructurebuilder.data.IBDataStreamSupplier;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.slf4j.Logger;

public final class DefaultIBDataIngester extends AbstractIBDataIngester implements IBDataIngester {

  private final Path cacheDirectory;

  public DefaultIBDataIngester(Path workingPath, Logger log, ConfigMap config, Path cacheDirectory) {
    super(workingPath, log, config);
    this.cacheDirectory = cacheDirectory;
  }

  @Override
  public List<IBDataStreamSupplier> ingest(IBDataSetIdentifier dsi, SortedMap<String, IBDataSourceSupplier> dssList) {
    requireNonNull(dsi);
    Date now = new Date(); // Ok for "now"  (Get it?)
    List<IBDataStreamSupplier> ibdssList = requireNonNull(dssList).values().stream().map(dss -> {
      IBDataSource source = dss.get()
          // Set the working _path
          .withTargetPath(getWorkingPath())
          // Name or nothing
          .withName(dsi.getName().orElse(null))
          // description or nothing
          .withDescription(dsi.getDescription().orElse(null))
          // Supply the default cache location
          .withDownloadCacheDirectory(getCacheDirectory());

      //        IBChecksumPathType thisOne = cet
      //            .withReturningTranslation(() -> copyToDeletedOnExitTempChecksumAndPath(of(workingPath), IBDATA_PREFIX,
      //                IBDATA_SUFFIX, source.getSourceURL().openStream()));

      return source.get().map(thisOne -> {

        Path localPath = thisOne.getPath();
        Optional<String> p = Optional.of(getWorkingPath().relativize(localPath).toString());

        source.getChecksum().ifPresent(checksum -> {
          if (!thisOne.getChecksum().equals(checksum))
            throw new IBDataException("Read stream failed to match expected checksum " + checksum);
        });
        DefaultIBDataStreamIdentifier ddsi = new DefaultIBDataStreamIdentifier(source, now, p);
        return new DefaultIBDataStreamSupplier(new DefaultIBDataStream(ddsi, thisOne));
      });

    })
        // JAVA 11+ .flatMap(Optional::stream)
        .filter(Optional::isPresent) // JAVA 8
        .map(Optional::get)
        //

        .collect(toList());

    return ibdssList;
  }

  protected Path getCacheDirectory() {
    return cacheDirectory;
  }
}
