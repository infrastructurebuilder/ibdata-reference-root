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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An object containing the result of a transformation and any errors.
 * @author mykel.alvis
 *
 * @param <T>
 */
public class DefaultIBDataTransformationResult implements IBDataTransformationResult {

  private Optional<IBDataSet> dataSet;
  private List<IBDataTransformationError> errors;

  public DefaultIBDataTransformationResult(IBDataSet createdDataSet) {
    this(Optional.ofNullable(createdDataSet), new ArrayList<>());
  }

  public DefaultIBDataTransformationResult(Optional<IBDataSet> createdSet, List<IBDataTransformationError> errors) {
    this.dataSet = Objects.requireNonNull(createdSet);
    this.errors = Objects.requireNonNull(errors);
  }

  @Override
  public List<IBDataTransformationError> getErrors() {
    return errors;
  }

  @Override
  public Optional<IBDataSet> get() {
    return dataSet;
  }

}
