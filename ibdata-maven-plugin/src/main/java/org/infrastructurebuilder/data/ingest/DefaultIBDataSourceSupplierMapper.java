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
package org.infrastructurebuilder.data.ingest;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.data.AbstractIBDataSourceSupplier;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBDataSource;
import org.infrastructurebuilder.data.IBDataSourceSupplier;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;

@Named
public class DefaultIBDataSourceSupplierMapper extends AbstractIBDataSourceSupplierMapper {
  public final static List<String> HEADERS = Arrays.asList("http://", "https://", "file:", "zip:");

  @Inject
  public DefaultIBDataSourceSupplierMapper(LoggerSupplier l, TypeToExtensionMapper t2e) {
    super(requireNonNull(l).get(), requireNonNull(t2e));
  }

  public List<String> getHeaders() {
    return HEADERS;
  }

  @Override
  public IBDataSourceSupplier getSupplierFor(DefaultIBDataStreamIdentifierConfigBean v) {
    return new DefaultIBDataSourceSupplier(v.getTemporaryId(),
        new DefaultIBDataSource(getLog(),
            v.getURL().orElseThrow(() -> new IBDataException("No url for " + v.getTemporaryId())), v.getName(),
            v.getDescription(), ofNullable(v.getChecksum()), of(v.getMetadata()), ofNullable(v.getMimeType()),
            getMapper()));
  }


}
