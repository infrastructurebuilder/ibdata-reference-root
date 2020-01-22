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

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.infrastructurebuilder.data.IBDataConstants.IBDATA_WORKING_PATH_SUPPLIER;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.infrastructurebuilder.data.AbstractIBDataMavenComponent;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBDataIngesterSupplier;
import org.infrastructurebuilder.data.IBSchemaIngesterSupplier;
import org.infrastructurebuilder.data.IBDataSetFinalizer;
import org.infrastructurebuilder.data.IBDataSetFinalizerSupplier;
import org.infrastructurebuilder.data.IBDataStreamSupplier;
import org.infrastructurebuilder.data.IBSchemaDAOSupplier;
import org.infrastructurebuilder.data.IBStreamerFactory;
import org.infrastructurebuilder.util.artifacts.IBArtifactVersionMapper;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.IBResource;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;

@Named("ingest")
public final class IBDataIngestMavenComponent extends AbstractIBDataMavenComponent {

  private final Map<String, IBDataIngesterSupplier<?>> allIngesters;
  private final Map<String, IBSchemaIngesterSupplier<?>> allSchemaIngesters;
  private final IBDataSourceSupplierFactory dsSupplierFactory;
  private final IBSchemaSourceSupplierFactory ssSupplierFactory;
//  private final IBDataSchemaSupplierFactory dschemaSupplierFactory;

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
      // Versions mapper
      IBArtifactVersionMapper artifactVersionMapper,
      // Late-bound PathSupplier. Must be set in the executor before use
      @Named(IBDATA_WORKING_PATH_SUPPLIER) final PathSupplier workingPathSupplier,
      // The logger
      final Log log,
      // Mapper for extensions to mime types
      final TypeToExtensionMapper defaultTypeToExtensionMapper,
      // The configuration map. Does not include config from components
      @Named(ConfigMapSupplier.MAVEN_WITH_SERVERS) final ConfigMapSupplier mavenConfigMapSupplier,
      final Map<String, IBDataIngesterSupplier<?>> allIngesters,
      // All DataSetFinalizer suppliers
      final Map<String, IBDataSetFinalizerSupplier<?, ?>> allDSFinalizers,
      // Streamer factory
      final IBStreamerFactory streamerFactory,
      // DataSourceSupplier Factory
      final IBDataSourceSupplierFactory ibdssf,
      // SchemaSourceSupplier Factory
      final IBSchemaSourceSupplierFactory ibdschemasf,
      // Schema ingesters
      final Map<String, IBSchemaIngesterSupplier<?>> allSchemaIngesters) {
    super(artifactVersionMapper, workingPathSupplier, log, defaultTypeToExtensionMapper, mavenConfigMapSupplier,
        allDSFinalizers, streamerFactory);
    this.allIngesters = requireNonNull(allIngesters);
    this.allSchemaIngesters = requireNonNull(allSchemaIngesters);
    this.dsSupplierFactory = requireNonNull(ibdssf);
    this.ssSupplierFactory = requireNonNull(ibdschemasf);
  }

  @SuppressWarnings("unchecked")
  public IBResource ingest(DefaultIBIngestion ingest) throws MojoFailureException {
    MavenProject p = getProject().orElseThrow(() -> new MojoFailureException("No supplied project"));
    requireNonNull(ingest).getDataSet().injectGAV(p.getGroupId(), p.getArtifactId(), p.getVersion()); // Ugh...side
                                                                                                      // effects

    IBDataSetFinalizer<DefaultIBIngestion> finalizer;
    try {
      finalizer = (IBDataSetFinalizer<DefaultIBIngestion>) getDataSetFinalizerSupplier(ingest.getFinalizer(),
          ingest.getFinalizerConfig());
    } catch (ClassCastException e) {
      throw new MojoFailureException("Finalizer " + ingest.getFinalizer() + " was not considered viable", e);
    }

    ConfigMapSupplier theConfig = getConfigMapSupplier();

    Optional<String> theIngesterHint = ofNullable(requireNonNull(ingest).getIngester());

    List<IBDataStreamSupplier> dsSuppliers = theIngesterHint // Same as schema
        .flatMap(j -> ofNullable(this.allIngesters.get(j)))
        .orElseThrow(() -> new MojoFailureException("No ingester named " + theIngesterHint.orElse(null)))
        // Get a new instance of the ingester supplier from configuration
        .configure(theConfig)
        // Get the instance
        .get()
        // do the ingestion
        .ingest(dsSupplierFactory.mapIngestionToSourceSuppliers(ingest));

    // Ingest ALL defined schemas in the Ingestion
    Optional<String> theSchemaIngesterHint = ofNullable(requireNonNull(ingest).getSchemaIngester());

    Optional<IBSchemaIngesterSupplier<?>> theSchemaIngester = theSchemaIngesterHint // Only one ingester for a dataset
        .map(j -> {
          return ofNullable(this.allSchemaIngesters.get(j))
              .orElseThrow(() -> new IBDataException("No schema Ingester for " + j));
        });

    List<IBSchemaDAOSupplier> schemaSuppliers = theSchemaIngester // Only one ingester for a dataset
        .map(si -> si.configure(theConfig) // configure it
            .get() // get the actual schema ingester, which could be expensive
            .ingest(ssSupplierFactory.mapIngestionToSuppliers(ingest)) // Produces sorted set
            .stream().collect(toList()))
        .orElseGet(() -> emptyList());
    ;
    try {
      return finalizer.finalize(null, ingest, dsSuppliers, schemaSuppliers, getBaseDir());
    } catch (IOException e) {
      throw new MojoFailureException("Failed to finalize", e);
    }

  }

}
