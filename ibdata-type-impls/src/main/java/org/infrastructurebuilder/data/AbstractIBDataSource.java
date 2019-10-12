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

import static java.util.Objects.requireNonNull;

import java.net.URL;
import java.util.Optional;

import org.infrastructurebuilder.util.BasicCredentials;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.w3c.dom.Document;

abstract public class AbstractIBDataSource  implements IBDataSource{

  protected final String id;
  protected final URL source;
  protected final Optional<BasicCredentials> creds;
  protected final Optional<Checksum> checksum;
  protected final Optional<Document> metadata;

  public AbstractIBDataSource(String id, URL source, Optional<BasicCredentials> creds, Optional<Checksum> checksum,
      Optional<Document> metadata) {
    super();
    this.id = requireNonNull(id);
    this.source = requireNonNull(source);
    this.creds = requireNonNull(creds);
    this.checksum = requireNonNull(checksum);
    
    this.metadata = requireNonNull(metadata);

  }

  @Override
  public URL getSourceURL() {
    return source;
  }

  @Override
  public Optional<BasicCredentials> getCredentials() {
    return creds;
  }

  @Override
  public Optional<Checksum> getChecksum() {
    return checksum;
  }

  @Override
  public Optional<Document> getMetadata() {
    return metadata;
  }

  @Override
  public String getId() {
    return this.id;
  }

}