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

import org.infrastructurebuilder.data.AbstractIBSchemaSourceSupplier;
import org.infrastructurebuilder.data.IBSchemaSource;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;
import org.slf4j.Logger;

abstract public class AbstractIBSchemaSourceSupplierMapper implements IBSchemaSourceSupplierMapper {

  private final Logger log;
  private final TypeToExtensionMapper mapper;
  private final PathSupplier wps;

  public AbstractIBSchemaSourceSupplierMapper(Logger log, TypeToExtensionMapper mapper, PathSupplier wps) {
    this.log = requireNonNull(log);
    this.mapper = requireNonNull(mapper);
    this.wps = requireNonNull(wps);
  }

  @Override
  public boolean respondsTo(IBDataSchemaIngestionConfig v) {
    return false;
//    String u = Optional.ofNullable(v).flatMap(IBDataSchemaIngestionConfig::getUrl).orElse("");
//    return getHeaders().stream().anyMatch(h -> u.startsWith(h));
  }

//  abstract public List<String> getHeaders();

  public Logger getLog() {
    return log;
  }

  public TypeToExtensionMapper getMapper() {
    return mapper;
  }

  @Override
  public Path getWorkingPath() {
    return wps.get();
  }

  public class DefaultIBSchemaSourceSupplier extends AbstractIBSchemaSourceSupplier {

    public DefaultIBSchemaSourceSupplier(String id, IBSchemaSource src, PathSupplier workingPath) {
      super(id, src, workingPath);
    }

  }

}