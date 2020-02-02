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
import java.util.Optional;

import org.infrastructurebuilder.data.AbstractIBSchemaSourceSupplier;
import org.infrastructurebuilder.data.IBSchemaSource;
import org.infrastructurebuilder.util.BasicCredentials;
import org.infrastructurebuilder.util.CredentialsFactory;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;
import org.slf4j.Logger;

abstract public class AbstractIBSchemaSourceSupplierMapper<P> implements IBSchemaSourceSupplierMapper {

  private final IBRuntimeUtils ibr;

  public AbstractIBSchemaSourceSupplierMapper(IBRuntimeUtils ibr) {
    this.ibr = requireNonNull(ibr);
  }

  @Override
  public boolean respondsTo(IBDataSchemaIngestionConfig v) {
    return false;
//    String u = Optional.ofNullable(v).flatMap(IBDataSchemaIngestionConfig::getUrl).orElse("");
//    return getHeaders().stream().anyMatch(h -> u.startsWith(h));
  }

//  abstract public List<String> getHeaders();

  protected CredentialsFactory getCredentialsFactory() {
    return ibr;
  }

  public IBRuntimeUtils getRuntimeUtils() {
    return ibr;
  }

  public Optional<BasicCredentials> getCredentialsFor(String query) {
    return getCredentialsFactory().getCredentialsFor(query);
  }

  public Logger getLog() {
    return ibr.getLog();
  }

  public TypeToExtensionMapper getMapper() {
    return ibr;
  }

  @Override
  public Path getWorkingPath() {
    return ibr.getWorkingPath();
  }

  public class DefaultIBSchemaSourceSupplier extends AbstractIBSchemaSourceSupplier<P> {

    public DefaultIBSchemaSourceSupplier(String id, IBSchemaSource<P> src, PathSupplier workingPath,
        IBDataSchemaIngestionConfig cfg) {
      super(id, src, workingPath, cfg);
    }

  }

  @Override
  public void close() throws Exception {
  }
}