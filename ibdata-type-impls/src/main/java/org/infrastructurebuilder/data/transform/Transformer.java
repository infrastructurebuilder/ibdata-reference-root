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
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.infrastructurebuilder.data.IBDataStream;
import org.infrastructurebuilder.data.IBMetadataUtils;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.DefaultConfigMapSupplier;
import org.w3c.dom.Document;

public class Transformer {
  /**
   * Required id, used as the hint if no hint supplied
   */
  private String id;
  private String hint;
  private Map<String, String> configuration = new HashMap<>();
  private boolean failOnAnyError = true;
  private List<DataStreamMatcher> sources = new ArrayList<>();
  private XmlPlexusConfiguration targetStreamMetadata;

  public String getId() {
    return id;
  }

  public String getHint() {
    return Optional.ofNullable(this.hint).orElse(id);
  }

  public Map<String, String> getConfiguration() {
    return configuration;
  }

  public void setId(String id) {
    this.id = requireNonNull(id);
  }

  public void setHint(String hint) {
    this.hint = hint;
  }

  public void setConfiguration(Map<String, String> configuration) {
    this.configuration = requireNonNull(configuration);
  }

  public boolean isFailOnAnyError() {
    return failOnAnyError;
  }

  public ConfigMapSupplier getConfigurationAsConfigMapSupplier(ConfigMapSupplier defaults) {
    return new DefaultConfigMapSupplier(defaults).addConfiguration(this.configuration);
  }

  public List<DataStreamMatcher> getSources() {
    return sources;
  }

  public void setSources(List<DataStreamMatcher> sources) {
    this.sources = sources;
  }

  public void setTargetStreamMetadata(XmlPlexusConfiguration targetStreamMetadata) {
    this.targetStreamMetadata = targetStreamMetadata;
  }

  public XmlPlexusConfiguration getTargetStreamMetadata() {
    return targetStreamMetadata;
  }

  public Document getTargetStreamMetadataAsDocument() {
    return IBMetadataUtils.fromXpp3Dom.apply(targetStreamMetadata);
  }

  private boolean matchesSources(IBDataStream stream) {
    return getSources().size() == 0 ||  getSources().stream().anyMatch(s -> s.matches(stream));
  }

  public List<IBDataStream> asMatchingStreams(Collection<IBDataStream> streams) {
    return requireNonNull(streams).stream().filter(this::matchesSources).collect(toList());
  }
}
