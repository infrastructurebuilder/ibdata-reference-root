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
package org.infrastructurebuilder.data;

import static org.infrastructurebuilder.IBConstants.IBDATA_PREFIX;
import static org.infrastructurebuilder.IBConstants.IBDATA_SUFFIX;
import static org.infrastructurebuilder.util.files.DefaultIBResource.copyToDeletedOnExitTempChecksumAndPath;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.apache.avro.file.SeekableFileInput;
import org.apache.avro.file.SeekableInput;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.artifacts.ChecksumEnabled;
import org.infrastructurebuilder.util.artifacts.SourcePathEnabled;
import org.infrastructurebuilder.util.files.IBResource;

public final class DefaultSeekableInputFromInputStream implements SeekableInput, ChecksumEnabled, SourcePathEnabled {

  private final SeekableFileInput proxy;
  private final IBResource cset;

  public DefaultSeekableInputFromInputStream(Path targetPath, InputStream stream) throws IOException {
    cset = copyToDeletedOnExitTempChecksumAndPath(targetPath, IBDATA_PREFIX, IBDATA_SUFFIX, stream);
    proxy = new SeekableFileInput(cset.getPath().toFile());
  }

  @Override
  public void close() throws IOException {
    proxy.close();
  }

  @Override
  public void seek(long p) throws IOException {
    proxy.seek(p);
  }

  @Override
  public long tell() throws IOException {
    return proxy.tell();
  }

  @Override
  public long length() throws IOException {
    return proxy.length();
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return proxy.read(b, off, len);
  }

  @Override
  public Checksum asChecksum() {
    return cset.getChecksum();
  }

  @Override
  public java.nio.file.Path getSourcePath() {
    return cset.getPath();
  }
}
