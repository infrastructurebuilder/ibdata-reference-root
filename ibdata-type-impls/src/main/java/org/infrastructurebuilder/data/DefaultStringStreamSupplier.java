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

import static org.infrastructurebuilder.IBConstants.TEXT_CSV;
import static org.infrastructurebuilder.IBConstants.TEXT_PLAIN;
import static org.infrastructurebuilder.IBConstants.TEXT_PSV;
import static org.infrastructurebuilder.IBConstants.TEXT_TSV;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Named;

import org.infrastructurebuilder.util.IBUtils;

@Named
public class DefaultStringStreamSupplier implements IBDataSpecificStreamFactory {
  public final static List<String> TYPES = Arrays.asList(TEXT_CSV, TEXT_PSV, TEXT_TSV, TEXT_PLAIN);

  @Override
  public Optional<Stream<? extends Object>> from(IBDataStream ds) {
    if (ds == null || !getRespondTypes().contains(ds.getMimeType()))
      return Optional.empty();
    IBDataStream d = ds;

    try (InputStream ins = d.get()) {
      Stream<Object> s = (Stream<Object>) IBUtils.readInputStreamAsStringStream(ins).map(o -> (Object) o);
      return Optional.of(s);
    } catch (IOException e) {
      throw new IBDataException(e);
    }
  }

  @Override
  public List<String> getRespondTypes() {
    return TYPES;
  }

}
