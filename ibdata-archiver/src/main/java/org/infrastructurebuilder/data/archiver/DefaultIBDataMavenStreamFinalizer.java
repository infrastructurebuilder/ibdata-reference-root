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
package org.infrastructurebuilder.data.archiver;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.archiver.AbstractArchiveFinalizer;
import org.codehaus.plexus.archiver.ArchiveFinalizer;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.infrastructurebuilder.util.files.IBChecksumPathType;

@Named
public class DefaultIBDataMavenStreamFinalizer extends AbstractArchiveFinalizer implements ArchiveFinalizer {
  private final IBDataLateBindingFinalizerConfigSupplier configSupplier;
  private IBChecksumPathType config = null;
  private final Logger logger;

  @Inject
  public DefaultIBDataMavenStreamFinalizer(Logger logger, IBDataLateBindingFinalizerConfigSupplier cusplier) {
    this.configSupplier = requireNonNull(cusplier);
    this.logger = requireNonNull(logger);
  }

  @Override
  public void finalizeArchiveCreation(Archiver archiver) throws ArchiverException {
    config = Optional.ofNullable(configSupplier.get())
        .orElseThrow(() -> new ArchiverException("No config supplied for Stream finalizer"));
  }

  @Override
  public void finalizeArchiveExtraction(UnArchiver unarchiver) throws ArchiverException {
  }

  @SuppressWarnings("rawtypes")
  @Override
  public List getVirtualFiles() {
    return Collections.emptyList();
    //    return config.getDataSet().getStreams().stream().map(dataStream -> Paths.get()).collect(Collectors.toList());
  }

}
