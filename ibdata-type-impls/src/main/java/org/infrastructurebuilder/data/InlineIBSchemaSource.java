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

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.io.StringReader;
import java.util.List;
import java.util.Optional;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.infrastructurebuilder.data.model.PersistedIBSchema;
import org.infrastructurebuilder.data.model.io.xpp3.PersistedIBSchemaXpp3Reader;
import org.infrastructurebuilder.data.model.io.xpp3.PersistedIBSchemaXpp3Writer;
import org.infrastructurebuilder.util.BasicCredentials;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.slf4j.Logger;

public class InlineIBSchemaSource extends AbstractIBSchemaSource<Xpp3Dom> {

  public InlineIBSchemaSource(PathSupplier wps, Logger logger) {
    super(wps, logger);
  }

  private InlineIBSchemaSource(PathSupplier wps
  // logger
      , Logger logger
      // id
      , String id
      // name
      , Optional<String> name
      // desc
      , Optional<String> desc
      // creds
      , Optional<BasicCredentials> creds
      // checksum
      , Optional<Checksum> checksum
      // metadata
      , Optional<Metadata> metadata
      // config
      , Optional<ConfigMap> config) {
    super(wps, logger, id, name, desc, creds, checksum, metadata, config);
  }

  @Override
  public InlineIBSchemaSource configure(ConfigMap config) {
    String id = config.getOrDefault("id", "default");
    Optional<Metadata> metadata2 = empty();
    Optional<Checksum> checksum2 = empty();
    Optional<BasicCredentials> creds2 = empty();
    Optional<String> desc2 = empty();
    Optional<String> name2 = empty();
    return new InlineIBSchemaSource(getWps(), getLog(), id, name2, desc2, creds2, checksum2, metadata2, of(config));
  }

  @Override
  protected List<IBChecksumPathType> getInstance(PathSupplier workingPath, Optional<Xpp3Dom> inline) {
    PersistedIBSchemaXpp3Writer writer = new PersistedIBSchemaXpp3Writer();
    PersistedIBSchemaXpp3Reader reader = new PersistedIBSchemaXpp3Reader();
    PersistedIBSchema v = IBDataException.cet.withReturningTranslation(
        () -> reader.read(new StringReader(requireNonNull(inline, "inline schema").toString())));
    // TODO Write to disk
//    Path p = get
    return emptyList();
  }

}
