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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.infrastructurebuilder.data.model.DataSet;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class IngestionTest {

  private static final String DEFAULT = "default";
  private static final String DEFAULT_INGEST = "default-ingest";
  private Ingestion i;
  private DefaultIBDataSetIdentifier ds;
  private Map<String, Object> fc;
  private DataSet targetDs;
  private final XmlPlexusConfiguration metadata = new XmlPlexusConfiguration("metadata");
  BiFunction<? extends DataSet, ? extends DataSet, Boolean> equalser;

  @Before
  public void setUp() throws Exception {
    // FIXME Create local .equals() for DataSet. See <a href="https://github.com/infrastructurebuilder/ibcore-root/issues/4"> this issue</a>.
    // Eventually this BiFunction should be static and move to (probably) IBDataSetIdentifier
    equalser = (lhs, target) -> {
      if (target == null)
        return false;
      if (target == lhs)
        return true;
      if (target.getClass() != lhs.getClass())
        return false;
      DataSet rhs = (DataSet) target;
      return new EqualsBuilder()
          // GroupId
          .append(lhs.getArtifactId(), rhs.getArtifactId())
          // ArtifactId
          .append(lhs.getArtifactId(), rhs.getArtifactId())
          // Version (Maybe we want to convert to IBVersions to do the comparison?)
          .append(lhs.getVersion(), rhs.getVersion()).isEquals();
    };

    fc = new HashMap<>();
    targetDs = new DataSet();
    i = new Ingestion();

    DefaultIBDataSetIdentifier ids = i.getDataSet();
    ids.setGroupId("A");
    ids.setArtifactId("B");
    ids.setVersion("1.0.0-SNAPSHOT");
    ids.setMetadata(metadata);
    ids.setName("name");
    ids.setDescription("description");
    ids.setPath("/");
    ds = new DefaultIBDataSetIdentifier(ids);

  }

  @Test
  public void testGetId() {
    assertEquals(DEFAULT, i.getId());
  }

  @Ignore // Need .equals to work properly
  @Test
  public void testGetDataSet() {
    assertEquals(ds, i.getDataSet());
  }

  @Test
  public void testGetIngester() {
    assertEquals(DEFAULT, i.getIngester());
  }

  @Test
  public void testGetFinalizer() {
    assertEquals(DEFAULT_INGEST, i.getFinalizer());
  }

  @Test
  public void testGetFinalizerConfig() {
    assertEquals(0, i.getFinalizerConfig().size());
  }

  @Ignore // Need .equals to work properly
  @Test
  public void testAsDataSet() {
    assertEquals(targetDs, i.asDataSet());
  }

}
