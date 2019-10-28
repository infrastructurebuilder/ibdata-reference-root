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

import static org.infrastructurebuilder.data.IBDataConstants.INGESTION_TARGET;
import static org.infrastructurebuilder.data.IBDataConstants.TRANSFORMATION_TARGET;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.infrastructurebuilder.data.AbstractIBDataMojo;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
@Mojo(name = "transform", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresProject = true)
public class IBDataTransformMojo extends AbstractIBDataMojo {

  @Parameter(required = true)
  private List<Transformation> transformations = new ArrayList<>();

  @Component
  IBDataTransformMavenComponent component;

  @Override
  protected IBDataTransformMavenComponent getComponent() {
    return component;
  }
  @SuppressWarnings("unchecked")
  @Override
  protected void _execute() throws MojoExecutionException, MojoFailureException {
    // Note: component.transform translates all IBDataExecptions into MojoFailureExceptions
    IBChecksumPathType thePath = component.transform(transformations);
    getLog().debug("Setting plugin context");
    @SuppressWarnings("rawtypes")
    final Map pc = getPluginContext();
    pc.put(TRANSFORMATION_TARGET, thePath);
    setPluginContext(pc);
    writeMarker(TRANSFORMATION_TARGET, thePath);

    getLog().debug("plugin context set");
    getLog().info("Data transformations complete with "
        + thePath.getPath() + " as " + thePath.getChecksum());
  }


}
