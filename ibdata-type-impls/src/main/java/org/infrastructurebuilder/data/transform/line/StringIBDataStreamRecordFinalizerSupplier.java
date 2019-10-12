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

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static org.infrastructurebuilder.data.IBDataException.cet;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.data.IBDataDataStreamRecordFinalizerSupplier;
import org.infrastructurebuilder.data.IBDataStreamRecordFinalizer;
import org.infrastructurebuilder.data.IBMetadataUtils;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;

@Named(StringIBDataStreamRecordFinalizerSupplier.NAME)
public class StringIBDataStreamRecordFinalizerSupplier extends AbstractIBDataStreamRecordFinalizerSupplier<String> {

  public static final String NAME = "string-finalizer";

  @Inject
  public StringIBDataStreamRecordFinalizerSupplier(
      @Named(IBMetadataUtils.IBDATA_WORKING_PATH_SUPPLIER) PathSupplier wps) {
    this(wps, null);
  }

  private StringIBDataStreamRecordFinalizerSupplier(PathSupplier ps, ConfigMapSupplier cms) {
    super(ps, cms);
  }

  @Override
  public IBDataDataStreamRecordFinalizerSupplier<String> config(ConfigMapSupplier cms) {
    return new StringIBDataStreamRecordFinalizerSupplier(getWps(), cms);
  }

  @Override
  public IBDataStreamRecordFinalizer<String> get() {
    // The working path needs to be stable and pre-existent
    return new StringIBDataStreamRecordFinalizer(NAME, getWps().get().resolve(UUID.randomUUID().toString()), getCms().get());
  }

  private class StringIBDataStreamRecordFinalizer extends AbstractIBDataStreamRecordFinalizer<String, BufferedWriter> {

    public StringIBDataStreamRecordFinalizer(String id, Path workingPath, Map<String, String> map) {
      super(id, workingPath, map,
          Optional.of(cet.withReturningTranslation(() -> Files.newBufferedWriter(workingPath, CREATE_NEW))));
    }

    @Override
    protected void writeThrows(String recordToWrite) throws Throwable {
      getWriter().write(recordToWrite);
      getWriter().write("\n");
    }

  }

}
