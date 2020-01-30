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

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.infrastructurebuilder.data.IBMetadataUtils.translateToMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.ingest.IBDataSchemaIngestionConfig;
import org.infrastructurebuilder.data.model.DataSet;
import org.infrastructurebuilder.util.config.ConfigMap;

public class DefaultIBTransformation implements IBTransformation {

  private String id = DEFAULT;
  private List<Transformer> transformers = new ArrayList<>();
  private String finalizer = null;
  private Map<String, String> finalizerConfig = new HashMap<>();

  // Not set with plugin config
  private String groupId, artifactId, version, name, description;
  private XmlPlexusConfiguration metadata;

  public DefaultIBTransformation() {
  }

  public DefaultIBTransformation(DefaultIBTransformation d, String groupId2, String artifactId2, String version2) {
    this();
    this.id = d.id;
    this.transformers = d.transformers.stream().collect(Collectors.toList());
    this.finalizer = d.finalizer;
    this.finalizerConfig = d.finalizerConfig.entrySet().stream()
        .collect(Collectors.toMap(k -> k.getKey(), v -> v.getValue()));
    this.groupId = groupId2;
    this.artifactId = artifactId2;
    this.version = version2;
    this.name = d.name;
    this.description = d.description;
    this.metadata = d.metadata;
    if (this.metadata == null)
      this.metadata = new XmlPlexusConfiguration("metadata"); // FIXME Where do we make metadata happen for a
                                                              // transformer
  }

  @Override
  public String toString() {
    return "Transformation [id=" + id + ", transformers=" + transformers + ", finalizer=" + finalizer + ", groupId="
        + groupId + ", artifactId=" + artifactId + ", version=" + version + ", name=" + name + ", metadata=" + metadata
        + "]";
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setTransformers(List<Transformer> transformers) {
    this.transformers = transformers;
  }

  public void setFinalizer(String finalizer) {
    this.finalizer = finalizer;
  }

  public void setFinalizerConfig(Map<String, String> finalizerConfig) {
    this.finalizerConfig = finalizerConfig;
  }

  public void setMetadata(XmlPlexusConfiguration metadata) {
    this.metadata = metadata;
  }

  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public List<Transformer> getTransformers() {
    return transformers.stream().map(t -> t.copy(this)).collect(Collectors.toList());
  }

  @Override
  public String getFinalizer() {
    return ofNullable(finalizer).orElse(DEFAULT_TRANSFORM);
  }

  @Override
  public ConfigMap getFinalizerConfig() {
    return new ConfigMap(finalizerConfig.entrySet().stream().collect(toMap(k -> k.getKey(), v -> v.getValue())));
  }

  public DefaultIBTransformation withIdentifiers(String groupId, String artifactId, String version) {
    return this.copy(groupId, artifactId, version);
  }

  private DefaultIBTransformation copy(String groupId2, String artifactId2, String version2) {
    return new DefaultIBTransformation(this, groupId2, artifactId2, version2);
  }

  @Override
  public DataSet asDataSet() {
    DataSet dsi = new DataSet();
    dsi.setGroupId(this.groupId);
    dsi.setArtifactId(this.artifactId);
    dsi.setVersion(this.version);
    dsi.setName(this.name);
    dsi.setDescription(this.description);
    dsi.setMetadata(translateToMetadata.apply(metadata));
    return dsi;
  }

  @Override
  public SortedMap<String, IBDataSchemaIngestionConfig> asSchemaIngestion() {
    throw new IBDataException("Cannot currently define shemas in transformation");
  }

}