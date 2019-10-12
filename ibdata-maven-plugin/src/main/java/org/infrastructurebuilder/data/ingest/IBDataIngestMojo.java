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

import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.infrastructurebuilder.data.AbstractIBDataMojo;
import org.infrastructurebuilder.data.LocalProxyInfoSupplier;
import org.infrastructurebuilder.data.ProxyInfoSupplier;
import org.infrastructurebuilder.util.files.IBChecksumPathType;

@Mojo(name = "ingest", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresProject = true)
public class IBDataIngestMojo extends AbstractIBDataMojo {

  @Parameter(required = true)
  private Ingestion ingest;

  @Component
  IBDataIngestMavenComponent component;

  @Override
  protected IBDataIngestMavenComponent getComponent() {
    return component;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void _execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Attempting Data Ingestion");
    final Map pc = getPluginContext();
    if (pc.containsKey(TRANSFORMATION_TARGET))
      throw new MojoFailureException("Transformation and Ingestion cannot be performed in the same module build.");
    IBChecksumPathType thePath = component.ingest(ingest);
    pc.put(INGESTION_TARGET, thePath);
    setPluginContext(pc);
    getLog().info("Data ingestion is complete with " + thePath.getPath() + " as " + thePath.getChecksum());
  }

}