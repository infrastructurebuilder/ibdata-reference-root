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
package org.infrastructurebuilder.data.transform.line;

import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.IBConstants;
import org.infrastructurebuilder.data.IBDataDataStreamRecordFinalizerSupplier;
import org.infrastructurebuilder.data.IBDataProtobufUtils;
import org.infrastructurebuilder.data.IBDataStreamRecordFinalizer;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.slf4j.Logger;

@Named(GenericProtobufIBDataRecordFinalizerSupplier.NAME)
public class GenericProtobufIBDataRecordFinalizerSupplier
    extends AbstractIBDataStreamRecordFinalizerSupplier<com.google.protobuf.GeneratedMessageV3> {

  public static final String NAME = "protobuf-typed";
  private static final List<Class<?>> ACCEPTABLE_TYPES = Arrays.asList(com.google.protobuf.GeneratedMessageV3.class);

  @Inject
  public GenericProtobufIBDataRecordFinalizerSupplier(@Named(IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps,
      LoggerSupplier l) {
    this(wps, l, null);
  }

  private GenericProtobufIBDataRecordFinalizerSupplier(IBRuntimeUtils ibr,  ConfigMapSupplier cms) {
    super(ps, l, cms);
  }

  @Override
  public IBDataDataStreamRecordFinalizerSupplier<com.google.protobuf.GeneratedMessageV3> configure(
      ConfigMapSupplier cms) {
    return new GenericProtobufIBDataRecordFinalizerSupplier(getRuntimeUtils(), cms);
  }

  @Override
  public IBDataStreamRecordFinalizer<com.google.protobuf.GeneratedMessageV3> get() {
    // The working path needs to be stable and pre-existent
    return new GenericAvroIBDataStreamRecordFinalizer(NAME, getWorkingPathSupplier().get().resolve(UUID.randomUUID().toString()),
        getLog(), getCms().get());
  }

  private class GenericAvroIBDataStreamRecordFinalizer
      extends AbstractIBDataStreamRecordFinalizer<com.google.protobuf.GeneratedMessageV3, ProtobufDataFileWriter> {

    private final int numberOfRowsToSkip;

    public GenericAvroIBDataStreamRecordFinalizer(String id, Path workingPath, Logger l, ConfigMap map) {
      super(id, workingPath, l, map, Optional.of(IBDataProtobufUtils.fromMapAndWP.apply(workingPath, map)));
      this.numberOfRowsToSkip = Integer.parseInt(map.getOrDefault(NUMBER_OF_ROWS_TO_SKIP_PARAM, "0"));
    }

    @Override
    public int getNumberOfRowsToSkip() {
      return this.numberOfRowsToSkip;
    }

    @Override
    protected void writeThrows(com.google.protobuf.GeneratedMessageV3 recordToWrite) throws Throwable {
      getWriter().append(recordToWrite);
    }

    @Override
    public Optional<String> produces() {
      return Optional.of(IBConstants.AVRO_BINARY);
    }

    @Override
    public Optional<List<Class<?>>> accepts() {
      return Optional.of(ACCEPTABLE_TYPES);
    }

  }

}
