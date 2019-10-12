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

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.archiver.AbstractArchiveFinalizer;
import org.codehaus.plexus.archiver.ArchiveFinalizer;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.util.files.IBChecksumPathType;

@Named(DefaultIBDataMavenArchiveFinalizer.NAME)
public class DefaultIBDataMavenArchiveFinalizer extends AbstractArchiveFinalizer implements ArchiveFinalizer {

  public static final String NAME = "default-ibdata";
  private IBDataLateBindingFinalizerConfigSupplier configSupplier;
  private Logger logger;
  private IBChecksumPathType config = null;
  private List vFiles = Collections.emptyList();

  @Inject
  public DefaultIBDataMavenArchiveFinalizer(Logger logger, IBDataLateBindingFinalizerConfigSupplier cffconfigSupplier) {
    this.logger = Objects.requireNonNull(logger);
    configSupplier = Objects.requireNonNull(cffconfigSupplier);
  }

  /** {@inheritDoc} */
  @Override
  public void finalizeArchiveCreation(final Archiver archiver) throws ArchiverException {
    logger.info("Finalizing " + this + " with " + archiver);
      config  = Optional.ofNullable(configSupplier.get())
        .orElseThrow(() -> new ArchiverException("No finalizer config available"));
      vFiles = new ArrayList();
      IBDataException.cet.withReturningTranslation(() -> Files.newDirectoryStream(config.getPath())).forEach(vFiles::add);
      archiver.addDirectory(config.getPath().toFile());
//    // Only valid entry criteria
//    final Path path = config.getWorkingPath().resolve(IBDataEngine.IBDATA_IBDATASET_XML).toAbsolutePath();
//    path.getParent().toFile().mkdirs();
//    IBDataFinalizerOutputGenerator writer;
//
//    switch (config.getExecutionType()) {
//    case IBDataSetFinalizerConfig.INGESTER:
//      // Build the current data using the original data
//      writer = new DefaultIBDataIngestionWriter(config);
//      break;
//    case IBDataSetFinalizerConfig.TRANSFORMER:
//      writer = new DefaultIBDataTransformationWriter(config);
//      break;
//    default:
//      throw new ArchiverException("Invalid type " + config.getExecutionType());
//    }
//
//    try (OutputStream w = Files.newOutputStream(path, StandardOpenOption.CREATE)) {
//      writer.write(w, Optional.of(config.getWorkingPath()));
//    } catch (final IOException e) {
//      throw new ArchiverException("Failed to write metadata file", e);
//    }
//    archiver.addFile(path.toFile(), IBDataEngine.IBDATA_IBDATASET_XML);
  }

  /** {@inheritDoc} */
  @Override
  public void finalizeArchiveExtraction(UnArchiver unarchiver) throws ArchiverException {
    super.finalizeArchiveExtraction(unarchiver); // NO-op...for now
  }

  /** {@inheritDoc} */
  @SuppressWarnings("rawtypes")
  @Override
  public List getVirtualFiles() {
    logger.info("Getting " + vFiles.size() + " virtual files");
    return vFiles;
  }

}
