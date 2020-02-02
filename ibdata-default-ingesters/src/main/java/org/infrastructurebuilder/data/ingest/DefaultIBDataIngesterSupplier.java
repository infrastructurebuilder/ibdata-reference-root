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

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.infrastructurebuilder.data.DefaultIBDataStreamIdentifier.toIBDataStreamSupplier;

import java.util.Date;
import java.util.List;
import java.util.SortedMap;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.data.IBDataIngester;
import org.infrastructurebuilder.data.IBDataSource;
import org.infrastructurebuilder.data.IBDataSourceSupplier;
import org.infrastructurebuilder.data.IBDataStreamSupplier;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.DefaultConfigMapSupplier;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;

@Named(DefaultIBDataIngesterSupplier.NAME)
public class DefaultIBDataIngesterSupplier extends AbstractIBDataIngesterSupplier<String> {

  public static final String NAME = "default";

  @Inject
  public DefaultIBDataIngesterSupplier(IBRuntimeUtils ibr) {
    this(ibr, null);
  }

  private DefaultIBDataIngesterSupplier(IBRuntimeUtils ibr, ConfigMapSupplier cms) {
    super(ibr, cms);
  }

  @Override
  protected IBDataIngester getInstance(IBRuntimeUtils ibr, String in) {
    return new DefaultIBDataIngester(ibr, getConfig().get());
  }

  @Override
  public DefaultIBDataIngesterSupplier getConfiguredSupplier(ConfigMapSupplier config) {
    return new DefaultIBDataIngesterSupplier(getRuntimeUtils(), config);
  }

  public final class DefaultIBDataIngester extends AbstractIBDataIngester {

    public DefaultIBDataIngester(IBRuntimeUtils ibr, ConfigMap config) {
      super(ibr, config);
    }

    @Override
    public List<IBDataStreamSupplier> ingest(SortedMap<String, IBDataSourceSupplier<?>> dssList) {
      Date now = new Date(); // Ok for "now" (Get it?)
      List<IBDataSource<?>> list = requireNonNull(dssList, "List of IBDataSourceSupplier instances")
          // SortedMap values come out in order, because that's how it works
          .values().stream()
          // Get the IBDataSource
          .map(IBDataSourceSupplier::get)
          // Get CONFIGURED source
          .map(ds -> ds.configure(new DefaultConfigMapSupplier(getConfig()).get())).collect(toList());

      List<IBDataStreamSupplier> l2 =
          // map configured source to
          list.stream().flatMap(source -> {
            getLog().info(format("Mapping %s from %s", source.getId(), source.getSource().getUrl()));
            getLog().info("Source type is " + source.getClass().getCanonicalName());
            return source.get().stream()
                .map(ibPathChecksumType -> toIBDataStreamSupplier(getWorkingPath(), source, ibPathChecksumType, now));
          })
              // Collect to an ordered list
              .collect(toList());
      return l2;
    }

  }

}
