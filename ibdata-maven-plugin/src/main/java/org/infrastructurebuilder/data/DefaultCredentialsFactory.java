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

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toCollection;
import static org.infrastructurebuilder.util.artifacts.Weighted.comparator;

import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.util.BasicCredentials;
import org.infrastructurebuilder.util.CredentialsFactory;
import org.infrastructurebuilder.util.CredentialsSupplier;

@Named
public class DefaultCredentialsFactory implements CredentialsFactory {

  private final SortedSet<CredentialsSupplier> ss;

  @Inject
  public DefaultCredentialsFactory(Map<String, CredentialsSupplier> credentialsSuppliers) {
    this.ss = credentialsSuppliers.values().stream().sorted(comparator()).collect(toCollection(TreeSet::new));
  }

  @Override
  public Optional<BasicCredentials> getCredentialsFor(String query) {
    // TODO Clean this up!
    for (CredentialsSupplier s : ss) {
      Optional<BasicCredentials> b = s.getCredentialsFor(query);
      if (b.isPresent())
        return b;
    }
    return empty();
  }

}
