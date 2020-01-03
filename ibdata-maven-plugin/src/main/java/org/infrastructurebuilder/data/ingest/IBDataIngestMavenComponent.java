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

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.infrastructurebuilder.data.AbstractIBDataMavenComponent;
import org.infrastructurebuilder.data.IBDataIngesterSupplier;
import org.infrastructurebuilder.data.IBDataSchemaIngesterSupplier;
import org.infrastructurebuilder.data.IBDataSetFinalizer;
import org.infrastructurebuilder.data.IBDataSetFinalizerSupplier;
import org.infrastructurebuilder.data.IBDataStream;
import org.infrastructurebuilder.data.IBStreamerFactory;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;

@Named("ingest")
public final class IBDataIngestMavenComponent extends AbstractIBDataMavenComponent {

  private final Map<String, IBDataIngesterSupplier> allIngesters;
  private final IBDataSourceSupplierFactory dsSupplierFactory;
  private final Map<String, IBDataSchemaIngesterSupplier> allSchemaIngesters;

  /**
   * Injected constructor.
   *
   * @param workingPathSupplier          Late bound PathSupplier, which is setup
   *                                     within _setup() to contain the working
   *                                     directory
   * @param log                          Maven log
   * @param defaultTypeToExtensionMapper a TypeToExtensionMapper, which maps mime
   *                                     types to file extensions
   * @param mavenConfigMapSupplier       Supplier of Map/String/String instance
   *                                     which is bound to many (most) maven
   *                                     properties
   * @param allSchemaIngesters
   */
  @Inject
  public IBDataIngestMavenComponent(
      // Late-bound PathSupplier. Must be set in the executor before use
      @Named(IBDATA_WORKING_PATH_SUPPLIER) final PathSupplier workingPathSupplier,
      // The logger
      final Log log,
      // Mapper for extensions to mime types
      final TypeToExtensionMapper defaultTypeToExtensionMapper,
      // The configuration map. Does not include config from components
      @Named(ConfigMapSupplier.MAVEN) final ConfigMapSupplier mavenConfigMapSupplier,
      final Map<String, IBDataIngesterSupplier> allIngesters,
      // All DataSetFinalizer suppliers
      final Map<String, IBDataSetFinalizerSupplier<?>> allDSFinalizers,
      // Streamer factory
      final IBStreamerFactory streamerFactory,
      // DataSourceSupplier Factory
      final IBDataSourceSupplierFactory ibdssf,
      // Schema ingesters
      final Map<String, IBDataSchemaIngesterSupplier> allSchemaIngesters) {
    super(workingPathSupplier, log, defaultTypeToExtensionMapper, mavenConfigMapSupplier, allDSFinalizers,
        streamerFactory);
    this.allIngesters = requireNonNull(allIngesters);
    this.allSchemaIngesters = requireNonNull(allSchemaIngesters);
    this.dsSupplierFactory = requireNonNull(ibdssf);
  }

  @SuppressWarnings("unchecked")
  public IBChecksumPathType ingest(Ingestion ingest) throws MojoFailureException {
    MavenProject p = getProject().orElseThrow(() -> new MojoFailureException("No supplied project"));
    requireNonNull(ingest).getDataSet().injectGAV(p.getGroupId(), p.getArtifactId(), p.getVersion()); // Ugh...side
                                                                                                      // effects

    IBDataSetFinalizer<Ingestion> finalizer;
    try {
      finalizer = (IBDataSetFinalizer<Ingestion>) getDataSetFinalizerSupplier(ingest.getFinalizer(),
          ingest.getFinalizerConfig());
    } catch (ClassCastException e) {
      throw new MojoFailureException("Finalizer " + ingest.getFinalizer() + " was not considered viable", e);
    }

    // TODO Ingest ALL defined schemas in the Ingestion first, mapping their
    // temporary ids to real ids

    List<Supplier<IBDataStream>> dsSuppliers = ofNullable(requireNonNull(ingest).getIngester())
        .flatMap(j -> ofNullable(this.allIngesters.get(j)))
        .orElseThrow(() -> new MojoFailureException("No ingester named " + ingest.getIngester()))
        // Get a new instance of the ingester supplier from configuration
        .configure(getConfigMapSupplier())
        // Get the instance
        .get()
        // do the ingestion
        .ingest(dsSupplierFactory.mapIngestionToSourceSuppliers(ingest));
    try {
      return finalizer.finalize(null, ingest, dsSuppliers, getBaseDir());
    } catch (IOException e) {
      throw new MojoFailureException("Failed to finalize", e);
    }

  }

}
