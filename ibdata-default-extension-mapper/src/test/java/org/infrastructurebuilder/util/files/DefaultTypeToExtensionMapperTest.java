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
package org.infrastructurebuilder.util.files;

import static org.infrastructurebuilder.IBConstants.APPLICATION_LIQUIBASE_CHANGELOG;
import static org.infrastructurebuilder.IBConstants.APPLICATION_XML;
import static org.infrastructurebuilder.IBConstants.DEFAULT_EXTENSION;
import static org.infrastructurebuilder.IBConstants.IBDATA_SCHEMA;
import static org.infrastructurebuilder.IBConstants.XML;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import org.infrastructurebuilder.data.util.files.DefaultTypeToExtensionMapper;
import org.junit.Before;
import org.junit.Test;

public class DefaultTypeToExtensionMapperTest {

  private TypeToExtensionMapper t2e;

  @Before
  public void setUp() throws Exception {
    t2e = new DefaultTypeToExtensionMapper();
  }

  @Test
  public void testGetExtensionForType() {
    assertEquals(XML, t2e.getExtensionForType(APPLICATION_XML));
    assertEquals(DEFAULT_EXTENSION, t2e.getExtensionForType("text/whackadoodle"));
  }

  @Test
  public void testReverseMap1() {
    SortedSet<String> t = t2e.reverseMapFromExtension(XML);
    assertEquals(4, t.size());
    assertTrue(t.contains(APPLICATION_XML));
  }

}
