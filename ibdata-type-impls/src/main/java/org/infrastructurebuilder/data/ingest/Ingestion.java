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

import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.infrastructurebuilder.data.DataSetEnabled;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBDataSetIdentifier;
import org.infrastructurebuilder.data.IBMetadataUtils;
import org.infrastructurebuilder.data.model.DataSet;
import org.w3c.dom.Document;

public class Ingestion implements DataSetEnabled {

  String id = "default";
  String ingester = "default";
  DefaultIBDataSetIdentifier dataSet = new DefaultIBDataSetIdentifier();
  String finalizer = "default-ingest";
  Map<String, String> finalizerConfig = new HashMap<>();

  public String getId() {
    return id;
  }

  public DefaultIBDataSetIdentifier getDataSet() {
    return dataSet;
  }

  public String getIngester() {
    return ofNullable(this.ingester).orElse(getId());
  }

  public String getFinalizer() {
    return finalizer;
  }

  public Map<String, String> getFinalizerConfig() {
    return finalizerConfig;
  }

  @Override
  public DataSet asDataSet() {
    return getDataSet().asDataSet();
  }



  public static class IngestionDataSet {

    private String name = "default";
    private String description = null;
    private String path = null;
    private UUID id = null;

    private List<DefaultIBDataStreamIdentifierConfigBean> streams = new ArrayList<>();

    private XmlPlexusConfiguration metadata;

    public IngestionDataSet() {
    }

    public UUID getId() {
      return ofNullable(this.id).orElse(UUID.randomUUID());
    }

    public Optional<String> getName() {
      return ofNullable(this.name);
    }

    public Optional<String> getDescription() {
      return ofNullable(this.description);
    }

    public Date getCreationDate() {
      return new Date();
    }

    public Document getMetadata() {
      return IBMetadataUtils.fromXpp3Dom.apply(metadata);
    }

    public List<DefaultIBDataStreamIdentifierConfigBean> getStreams() {

      return streams.stream().collect(Collectors.toList());
    }

    public void setMetadata(XmlPlexusConfiguration metadata) {
      this.metadata = metadata;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public void setStreams(List<DefaultIBDataStreamIdentifierConfigBean> stream2s) {
      this.streams = stream2s;

    }

    public void setName(String name) {
      this.name = name;
    }

    public String getPath() {
      return this.path;
    }

    public void setPath(String path) {
      this.path = path;
    }

    public IBDataSetIdentifier asDataSetIdentifier() {
      throw new IBDataException("Not implemented");
      //      return new DefaultIBDataSetIdentifier(this);
    }

  }

}
