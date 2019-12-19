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
package org.infrastructurebuilder.data.transform.line;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.infrastructurebuilder.data.IBDataException;

import com.google.protobuf.GeneratedMessageV3;

public class ProtobufDataFileWriter implements Closeable {
  private final Path targetPath;
  private final OutputStream outputStream;

  public final static ProtobufDataFileWriter create(Path p) {
    return new ProtobufDataFileWriter(p);
  }

  public ProtobufDataFileWriter(Path targetPath) {
    this.targetPath = Objects.requireNonNull(targetPath).toAbsolutePath();
    if (Files.exists(this.targetPath))
      throw new IBDataException("Target path " + this.targetPath + " exists");
    this.outputStream = IBDataException.cet.withReturningTranslation(() -> Files.newOutputStream(this.targetPath));
  }

  public void append(GeneratedMessageV3 recordToWrite) {
    IBDataException.cet.withTranslation(() -> recordToWrite.writeTo(this.outputStream));
  }

  @Override
  public void close() throws IOException {
    this.outputStream.close();
  }

}
