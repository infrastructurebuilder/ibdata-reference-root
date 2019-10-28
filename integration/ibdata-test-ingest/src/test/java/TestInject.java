
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.infrastructurebuilder.data.IBDataSet;
import org.infrastructurebuilder.data.InjectIBData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(InjectIBData.class)
public class TestInject {

  private IBDataSet basic;

  public TestInject(IBDataSet injected) {
    this.basic = injected;
  }

  @Test
  public void didIGetOne() {
    assertNotNull(this.basic);
    assertEquals(
        "c369ba6d026298ec8b5d0915fe35582b89882b11b48a04d8ffff586ec78fa391291deb8b72dad8dbd84b07ef4d87aba332b0b08572b6113344958805226d7b4d",
        this.basic.getStreamSuppliers().get(0).get().asChecksum().toString());
  }

}
