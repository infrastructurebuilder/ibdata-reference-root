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
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.data.AbstractIBDataSchemaSource;
import org.infrastructurebuilder.data.IBDataSchemaIdentifier;
import org.infrastructurebuilder.data.IBDataSchemaSource;
import org.infrastructurebuilder.data.IBDataSchemaSupplier;
import org.infrastructurebuilder.data.Metadata;
import org.infrastructurebuilder.util.BasicCredentials;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;
import org.slf4j.Logger;

@Named
public class DefaultIBDataSchemaSupplierMapper extends AbstractIBDataSchemaSupplierMapper {
//  public final static List<String> HEADERS = Collections.emptyList();

  @Inject
  public DefaultIBDataSchemaSupplierMapper(LoggerSupplier l, TypeToExtensionMapper t2e,
      @Named(IBDATA_WORKING_PATH_SUPPLIER) PathSupplier workingPathSupplier) {
    super(requireNonNull(l).get(), requireNonNull(t2e), workingPathSupplier);
  }

  @Override
  public boolean respondsTo(IBDataSchemaIdentifier v) {

    return super.respondsTo(v);
  }

  @Override
  public IBDataSchemaSupplier getSupplierFor(String temporaryId, IBDataSchemaIdentifier v) {
    SortedSet<IBDataSchemaSource> theSet = Collections.emptySortedSet();

    return new DefaultIBDataSchemaSupplier(temporaryId // Temp id for this
        , theSet

        , getWorkingPath());
  }

  public class DefaultIBDataSchemaSource extends AbstractIBDataSchemaSource {

    private final Path targetPath;
    private final TypeToExtensionMapper mapper;
    private List<IBChecksumPathType> read;

    private DefaultIBDataSchemaSource(Logger log, String id, Optional<BasicCredentials> creds,
        Optional<Checksum> checksum, Optional<Metadata> metadata, Optional<ConfigMap> additionalConfig, Path targetPath,
        Optional<String> name, Optional<String> description, TypeToExtensionMapper mapper) {

      super(log, id, name, description, creds, checksum, metadata, additionalConfig);
      this.targetPath = targetPath;
      this.mapper = requireNonNull(mapper);
    }

    public DefaultIBDataSchemaSource(Logger log, Optional<String> name, Optional<String> description,
        Optional<Checksum> checksum, Optional<Metadata> metadata, TypeToExtensionMapper mapper) {
      this(log, randomUUID().toString(), empty(), checksum, metadata, empty(), null, name, description, mapper);
    }

    @Override
    public IBDataSchemaSource configure(ConfigMap config) {
      return new DefaultIBDataSchemaSource(getLog(), getId(), getCredentials(), getChecksum(), getMetadata(),
          of(config), getWorkingPath(), getName(), getDescription(), this.mapper);
    }

    @Override
    public List<IBChecksumPathType> getInstance() {
      return ofNullable(targetPath).map(target -> {
        if (this.read == null) {
          List<IBChecksumPathType> localRead = Collections.emptyList();
          read = localRead;
        }
        return read;
      }).orElse(Collections.emptyList());
    }

  }

}
