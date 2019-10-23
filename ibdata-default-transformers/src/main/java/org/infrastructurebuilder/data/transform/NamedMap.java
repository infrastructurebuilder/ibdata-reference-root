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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class NamedMap {
  Map<String,String> names = new HashMap<>();

  public void setNames(Map<String, String> names) {
    this.names = names;
  }

  public Map<String, String> getNames() {
    return names;
  }

  public String keyFor(String key, String defaultValue) {
    return Optional.ofNullable(names.getOrDefault(key, defaultValue)).orElse(null);
  }

}
