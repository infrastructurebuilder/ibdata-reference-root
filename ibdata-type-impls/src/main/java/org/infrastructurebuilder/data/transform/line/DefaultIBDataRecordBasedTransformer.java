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

import org.infrastructurebuilder.data.IBDataStreamRecordFinalizer;
import org.infrastructurebuilder.data.IBDataTransformer;
import org.slf4j.Logger;

@SuppressWarnings("rawtypes")
public class DefaultIBDataRecordBasedTransformer extends AbstractIBDataRecordBasedTransformer {


  public DefaultIBDataRecordBasedTransformer(Path p, Logger l,
      Map<String, IBDataRecordTransformerSupplier> dataRecTransformerSuppliers, IBDataStreamRecordFinalizer finalizer) {
    this(p, l, null, dataRecTransformerSuppliers, finalizer);
  }

  protected DefaultIBDataRecordBasedTransformer(Path workingPath, Logger l, Map<String, String> config,
      Map<String, IBDataRecordTransformerSupplier> dataRecTransformerSuppliers,
      // finalizer
      IBDataStreamRecordFinalizer finalizer) {
    super(workingPath, l, config, dataRecTransformerSuppliers, finalizer);
  }

  @Override
  public IBDataTransformer configure(Map<String, String> map) {
    return new DefaultIBDataRecordBasedTransformer(getWorkingPath(),getLog(), map, getDataLineSuppliers(),
        getConfiguredFinalizer());
  }


  @Override
  public String getHint() {
    return DefaultIBDataRecordBasedTransformerSupplier.RECORD_BASED_TRANSFORMER_SUPPLIER;
  }


}
