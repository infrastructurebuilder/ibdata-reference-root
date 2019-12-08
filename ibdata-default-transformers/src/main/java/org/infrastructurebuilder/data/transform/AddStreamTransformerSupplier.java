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
package org.infrastructurebuilder.data.transform;

import static java.util.Optional.ofNullable;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;
import static org.infrastructurebuilder.data.IBDataException.cet;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.data.DefaultIBDataSet;
import org.infrastructurebuilder.data.DefaultIBDataStream;
import org.infrastructurebuilder.data.DefaultIBDataStreamIdentifier;
import org.infrastructurebuilder.data.DefaultIBDataStreamSupplier;
import org.infrastructurebuilder.data.DefaultIBDataTransformationResult;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBDataSet;
import org.infrastructurebuilder.data.IBDataStream;
import org.infrastructurebuilder.data.IBDataStreamIdentifier;
import org.infrastructurebuilder.data.IBDataTransformationResult;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.slf4j.Logger;

@Named(AddStreamTransformerSupplier.ADD_STREAM)
public class AddStreamTransformerSupplier extends AbstractIBDataTransformerSupplier {
  public static final String ADD_STREAM = "add-stream";
  public static final String ADDED_PATH = "addedPath";

  @Inject
  public AddStreamTransformerSupplier(@Named(IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps,
      LoggerSupplier loggerSupplier) {
    this(wps, loggerSupplier, null);
  }

  private AddStreamTransformerSupplier(PathSupplier wps, LoggerSupplier loggerSupplier, ConfigMapSupplier cms) {
    super(wps, loggerSupplier, cms);
  }

  @Override
  public AddStreamTransformerSupplier configure(ConfigMapSupplier cms) {
    return new AddStreamTransformerSupplier(getWps(), () -> getLog(), cms);
  }

  @Override
  protected AddStreamTransformer getUnconfiguredTransformerInstance(Path workingPath) {
    return new AddStreamTransformer(getWps().get(), getLog());
  }

  public static class AddStreamTransformer extends AbstractIBDataTransformer {

    public AddStreamTransformer(Path path, Logger l) {
      this(path, l, null);
    }

    private AddStreamTransformer(Path path, Logger l, ConfigMap cm) {
      super(path, l, cm);
    }

    @Override
    public AddStreamTransformer configure(ConfigMap map) {
      return new AddStreamTransformer(getWorkingPath(), getLog(), map);
    }

    @Override
    public String getHint() {
      return ADD_STREAM;
    }

    @Override
    public IBDataTransformationResult transform(Transformer transformer, IBDataSet ds,
        List<IBDataStream> suppliedStreams, boolean failOnError) {
      Path targetPath = Paths.get((String) getConfig().getRequired(ADDED_PATH));
      String u = cet.withReturningTranslation(() -> targetPath.toUri().toURL().toExternalForm());

      IBDataSet createdDataSet = new DefaultIBDataSet(ds);
      IBDataStreamIdentifier identifier = new DefaultIBDataStreamIdentifier(null, Optional.of(u), Optional.empty(),
          Optional.empty(), new Checksum(targetPath), new Date(), transformer.getTargetStreamMetadataAsDocument(),
          transformer.getTargetMimeType(), Optional.of(targetPath.toAbsolutePath().toString()));
      createdDataSet.getStreamSuppliers()
          .add(new DefaultIBDataStreamSupplier(new DefaultIBDataStream(identifier, targetPath)));
      return new DefaultIBDataTransformationResult(createdDataSet, getWorkingPath());
    }

  }

}
