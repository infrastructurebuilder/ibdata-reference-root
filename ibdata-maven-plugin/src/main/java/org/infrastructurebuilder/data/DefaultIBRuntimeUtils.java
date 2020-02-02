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

import static org.infrastructurebuilder.IBConstants.DEFAULT;
import static org.infrastructurebuilder.IBConstants.MAVEN;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;
import static org.infrastructurebuilder.util.InjectedSLF4JFromMavenLoggerSupplier.LOG;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.infrastructurebuilder.util.CredentialsFactory;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.artifacts.GAV;
import org.infrastructurebuilder.util.artifacts.IBArtifactVersionMapper;
import org.infrastructurebuilder.util.config.AbstractIBRuntimeUtils;
import org.infrastructurebuilder.util.config.DependenciesSupplier;
import org.infrastructurebuilder.util.config.GAVSupplier;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;

@Named(IBDATA)
@Singleton
public class DefaultIBRuntimeUtils extends AbstractIBRuntimeUtils {

  private DependenciesSupplier ds;

  @Inject
  public DefaultIBRuntimeUtils(
      // WPS from maven run
      @Named(IBDATA_WORKING_PATH_SUPPLIER) IBDataWorkingPathSupplier wps
      // Maven logger as an SLF4J logger
      , @Named(LOG) LoggerSupplier ls
      // GAV from the Maven project
      , @Named(MAVEN) GAVSupplier gs
      // Default (all weighted creds suppliers) credentials factory
      , @Named(DEFAULT) CredentialsFactory cf
      // Dpe Supplier
      , @Named(MAVEN) DependenciesSupplier ds
      // Version mappers
      , @Named(DEFAULT) IBArtifactVersionMapper avm
      // t2em
      , TypeToExtensionMapper t2em) {
    super(wps, ls, gs, cf, avm, t2em);
    this.ds = Objects.requireNonNull(ds);
  }

  @Override
  public List<GAV> getDependencies() {
    return ds.get();
  }
}
