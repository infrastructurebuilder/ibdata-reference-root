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
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.IBConstants;
import org.infrastructurebuilder.data.DefaultIBDataStream;
import org.infrastructurebuilder.data.DefaultIBDataStreamIdentifier;
import org.infrastructurebuilder.data.DefaultIBDataStreamSupplier;
import org.infrastructurebuilder.data.DefaultTypeToExtensionMapper;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBDataIngester;
import org.infrastructurebuilder.data.IBDataSetIdentifier;
import org.infrastructurebuilder.data.IBDataSourceSupplier;
import org.infrastructurebuilder.data.IBDataStream;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.config.AbstractCMSConfigurableSupplier;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.slf4j.Logger;

@Named("jdbc")
public class DefaultIBDataJDBCIngesterSupplier extends AbstractIBDataIngesterSupplier {
  public static final String QUERY = "query";

  @Inject
  public DefaultIBDataJDBCIngesterSupplier(@Named(IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps,
      @Named(ConfigMapSupplier.MAVEN) ConfigMapSupplier cms, LoggerSupplier log) {
    this(wps, log, cms);
  }

  private DefaultIBDataJDBCIngesterSupplier(PathSupplier wps, LoggerSupplier log, ConfigMapSupplier cms) {
    super(wps, log, cms);
  }

  @Override
  public AbstractCMSConfigurableSupplier<IBDataIngester> getConfiguredSupplier(ConfigMapSupplier cms) {
    return new DefaultIBDataJDBCIngesterSupplier(getWps(), () -> getLog(), cms);
  }

  @Override
  protected IBDataIngester configuredType(ConfigMapSupplier config) {
    return new DefaultIBDataJDBCIngester(getWps().get(), getLog(), config.get());
  }

  public final class DefaultIBDataJDBCIngester extends AbstractIBDataIngester {

    private final String sql;
    private DSLContext create;
    private final String url;

    public DefaultIBDataJDBCIngester(Path workingPath, Logger l, ConfigMap config) {
      super(workingPath, l, config);
      sql = config.getString(QUERY);
      url = config.getString("url");

    }

    @Override
    public List<Supplier<IBDataStream>> ingest(Ingestion ingest, IBDataSetIdentifier dsi,
        SortedMap<String, IBDataSourceSupplier> dssList) {
      requireNonNull(dsi);
      try (DefaultDatabaseIBDataSource source = new DefaultDatabaseIBDataSource(getLog(), new URL(url),
          Optional.empty(), Optional.empty(), Optional.of(IBConstants.AVRO_BINARY), new DefaultTypeToExtensionMapper())
              //          DefaultDatabaseIBDataSource source = new DefaultDatabaseIBDataSource(ingest.getId(), url, sql)// Set the working _path
              .withTargetPath(getWorkingPath())
              // Name or nothing
              .withName(dsi.getName().orElse(null))
              // description or nothing
              .withDescription(dsi.getDescription().orElse(null))) {
        Date now = new Date(); // Ok for "now"  (Get it?)

        Result<Record> res = create.fetch(sql);

        ;
        for (Record r : res) {
          String s = r.format();

        }

        List<Supplier<IBDataStream>> ibdssList = requireNonNull(dssList).values().stream().map(dss -> {
          //          IBDataSource source = dss.get()

          //
          ;

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
      } catch (SQLException | MalformedURLException e) {
        throw new IBDataException(e);
      } catch (Exception e1) {
        throw new IBDataException(e1);
      }
    }

  }

}
