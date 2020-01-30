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

import org.apache.commons.dbcp2.BasicDataSource;
import org.infrastructurebuilder.util.CredentialsFactory;
import org.infrastructurebuilder.util.URLAndCreds;

public class IBDataConfiguredDataSource extends BasicDataSource {

  public IBDataConfiguredDataSource(String driverClass, URLAndCreds u, CredentialsFactory cf) {
    setDriverClassName(driverClass);
    setUrl(u.getUrl());
    requireNonNull(cf).getCredentialsFor(u).ifPresent(cr -> {
      setUsername(cr.getKeyId());
      cr.getSecret().ifPresent(secret -> setPassword(secret));
    });

  }
}
