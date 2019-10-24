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
package org.infrastructurebuilder.data.archiver;

import java.nio.file.Path;

import org.infrastructurebuilder.data.IBMetadataUtils;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.infrastructurebuilder.util.files.DefaultIBChecksumPathType;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractIBDataConfigTestSetup {
  protected IBDataLateBindingFinalizerConfigSupplier c;
  protected Logger logger;
  private IBChecksumPathType t;
  private Path p;
  private Checksum csum;
  private String type;
  private TestingPathSupplier wps = new TestingPathSupplier();

 public void abstractSetup() {
   Path p1 = wps.get();
   t= DefaultIBChecksumPathType.from(p1, new Checksum(), IBMetadataUtils.APPLICATION_IBDATA_ARCHIVE);
   c = new IBDataLateBindingFinalizerConfigSupplier();
   c.setT(t);
 }
}
