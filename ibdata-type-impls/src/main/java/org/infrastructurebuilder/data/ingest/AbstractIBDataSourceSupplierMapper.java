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

import java.util.List;
import java.util.Optional;

import org.infrastructurebuilder.data.AbstractIBDataSourceSupplier;
import org.infrastructurebuilder.data.IBDataSource;
import org.infrastructurebuilder.data.IBDataStreamIdentifier;
import org.infrastructurebuilder.data.IBDatabaseDialectMapper;
import org.infrastructurebuilder.util.BasicCredentials;
import org.infrastructurebuilder.util.CredentialsFactory;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;
import org.slf4j.Logger;

abstract public class AbstractIBDataSourceSupplierMapper<T> implements IBDataSourceSupplierMapper {

  private final IBRuntimeUtils ibr;

  public AbstractIBDataSourceSupplierMapper(IBRuntimeUtils ibr) {
    this.ibr = requireNonNull(ibr);
  }

  public IBRuntimeUtils getRuntimeUtils() {
    return ibr;
  }

  @Override
  public boolean respondsTo(IBDataStreamIdentifier v) {
    String u = Optional.ofNullable(v).flatMap(IBDataStreamIdentifier::getUrl).orElse("");
    return getHeaders().stream().anyMatch(h -> u.startsWith(h));
  }

  abstract public List<String> getHeaders();

  public Logger getLog() {
    return ibr.getLog();
  }

  public TypeToExtensionMapper getMapper() {
    return ibr;
  }

  public CredentialsFactory getCredentialsFactory() {
    return ibr;
  }

  @Override
  public PathSupplier getWorkingPathSupplier() {
    return () -> ibr.getWorkingPath();
  }

  public class DefaultIBDataSourceSupplier<T> extends AbstractIBDataSourceSupplier<T> {

    public DefaultIBDataSourceSupplier(String id, IBDataSource<T> src, PathSupplier wps,
        Optional<BasicCredentials> creds) {
      super(id, src, wps, creds);
    }

  }

}