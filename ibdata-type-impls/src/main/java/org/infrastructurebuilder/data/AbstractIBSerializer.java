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
//package org.infrastructurebuilder.data;
//
//import static java.util.Objects.requireNonNull;
//import static java.util.Optional.ofNullable;
//
//import java.nio.file.Path;
//import java.util.Optional;
//
//abstract public class AbstractIBSerializer<T, C, S extends AutoCloseable> implements IBSerializer<T, C, S> {
//
//  private final Optional<Path> path;
//  private final Optional<C> config;
//
//  protected AbstractIBSerializer(Optional<Path> p, Optional<C> c) {
//    this.path = requireNonNull(p);
//    this.config = requireNonNull(c);
//  }
//
//  protected Optional<C> getConfig() {
//    return config;
//  }
//
//  protected Optional<Path> getPath() {
//    return path;
//  }
//
//  abstract protected IBSerializer<T, C, S> newInstance(Optional<Path> p, Optional<C> c);
//
//  @Override
//  public IBSerializer<T, C, S> toPath(Path p) {
//    return newInstance(ofNullable(p), config);
//  }
//
//  @Override
//  public IBSerializer<T, C, S> withSerializationConfiguration(C c) {
//    return newInstance(path, ofNullable(c));
//  }
//
//}
