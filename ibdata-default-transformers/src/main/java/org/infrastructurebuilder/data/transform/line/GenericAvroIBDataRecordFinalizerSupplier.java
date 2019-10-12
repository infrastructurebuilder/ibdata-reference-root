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

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericRecord;
import org.infrastructurebuilder.data.IBDataAvroUtils;
import org.infrastructurebuilder.data.IBDataDataStreamRecordFinalizerSupplier;
import org.infrastructurebuilder.data.IBDataStreamRecordFinalizer;
import org.infrastructurebuilder.data.IBMetadataUtils;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;

@Named(GenericAvroIBDataRecordFinalizerSupplier.NAME)
public class GenericAvroIBDataRecordFinalizerSupplier extends AbstractIBDataStreamRecordFinalizerSupplier<GenericRecord> {

  public static final String NAME = "avro-finalizer";

  @Inject
  public GenericAvroIBDataRecordFinalizerSupplier(
      @Named(IBMetadataUtils.IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps) {
    this(wps, null);
  }

  private GenericAvroIBDataRecordFinalizerSupplier(PathSupplier ps, ConfigMapSupplier cms) {
    super(ps, cms);
  }

  @Override
  public IBDataDataStreamRecordFinalizerSupplier<GenericRecord> config(ConfigMapSupplier cms) {
    return new GenericAvroIBDataRecordFinalizerSupplier(getWps(), cms);
  }

  @Override
  public IBDataStreamRecordFinalizer<GenericRecord> get() {
    // The working path needs to be stable and pre-existent
    return new GenericAvroIBDataStreamRecordFinalizer(NAME, getWps().get().resolve(UUID.randomUUID().toString()), getCms().get());
  }

  private class GenericAvroIBDataStreamRecordFinalizer
      extends AbstractIBDataStreamRecordFinalizer<GenericRecord, DataFileWriter<GenericRecord>> {

    public GenericAvroIBDataStreamRecordFinalizer(String id, Path workingPath, Map<String, String> map) {
      super(id, workingPath, map, Optional.of(IBDataAvroUtils.fromMapAndWP.apply(workingPath, map)));
    }

    @Override
    protected void writeThrows(GenericRecord recordToWrite) throws Throwable {
      getWriter().append(recordToWrite);
    }

  }

}
