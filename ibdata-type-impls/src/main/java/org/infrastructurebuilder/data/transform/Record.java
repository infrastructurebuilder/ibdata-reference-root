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

import static java.util.Objects.requireNonNull;
import static org.infrastructurebuilder.data.IBMetadataUtils.MAP_SPLITTER;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Record {

  String id;
  String hint;
  Map<String, String> config = new HashMap<>();

  public String getId() {
    return id;
  }

  public String getHint() {
    return Optional.ofNullable(hint).orElse(id);
  }

  public Map<String, String> getConfig() {
    return config;
  }

  public String joinKey() {
    return requireNonNull(getId()) + MAP_SPLITTER + requireNonNull(getHint());
  }
  public Map<String, String> configurationAsMap() {
    Map<String,String> map = new HashMap<>();
    map.putAll(getConfig().entrySet().stream().collect(Collectors.toMap(
        // Key is id + hint (possibly same) joined by a splittable item
        k ->  joinKey() + "." + k.getKey(),
        v -> v.getValue())));
    return map;
  }

}
