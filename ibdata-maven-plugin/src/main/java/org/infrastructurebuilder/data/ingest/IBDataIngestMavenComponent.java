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
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.infrastructurebuilder.data.IBDataException.cet;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.infrastructurebuilder.data.AbstractIBDataMavenComponent;
import org.infrastructurebuilder.data.IBDataIngesterSupplier;
import org.infrastructurebuilder.data.IBDataSetFinalizer;
import org.infrastructurebuilder.data.IBDataSetFinalizerSupplier;
import org.infrastructurebuilder.data.IBDataSourceSupplier;
import org.infrastructurebuilder.data.IBDataStreamSupplier;
import org.infrastructurebuilder.data.IBMetadataUtils;
import org.infrastructurebuilder.data.IBStreamerFactory;
import org.infrastructurebuilder.data.archiver.IBDataLateBindingFinalizerConfigSupplier;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;
import org.infrastructurebuilder.util.logging.SLF4JFromMavenLogger;
import org.slf4j.Logger;

@Named("ingest")
public final class IBDataIngestMavenComponent extends AbstractIBDataMavenComponent {

  private final Map<String, IBDataIngesterSupplier> allIngesters;

  /**
   * Injected constructor.
   * @param workingPathSupplier  Late bound PathSupplier, which is setup within _setup() to contain the working directory
   * @param log Maven log
   * @param defaultTypeToExtensionMapper a TypeToExtensionMapper, which maps mime types to file extensions
   * @param mavenConfigMapSupplier Supplier of Map/String/String instance which is bound to many (most) maven properties
   */
  @Inject
  public IBDataIngestMavenComponent(
      // Late-bound  PathSupplier.  Must be set in the executor before use
      @Named(IBMetadataUtils.IBDATA_WORKING_PATH_SUPPLIER) PathSupplier workingPathSupplier,
      // The logger
      Log log,
      // Mapper for extensions to mime types
      TypeToExtensionMapper defaultTypeToExtensionMapper,
      // The configuration map. Does not include config from components
      @Named(ConfigMapSupplier.MAVEN) ConfigMapSupplier mavenConfigMapSupplier,
      //
      @Named(IBDataLateBindingFinalizerConfigSupplier.NAME) IBDataLateBindingFinalizerConfigSupplier configSupplier,
      Map<String, IBDataIngesterSupplier> allIngesters,
      // All DataSetFinalizer suppliers
      Map<String, IBDataSetFinalizerSupplier> allDSFinalizers,
      final IBStreamerFactory streamerFactory) {
    super(workingPathSupplier, log, defaultTypeToExtensionMapper, mavenConfigMapSupplier, allDSFinalizers, streamerFactory);
    this.allIngesters = requireNonNull(allIngesters);
  }

  public final static SortedMap<String, IBDataSourceSupplier> mapIngestionToSourceSuppliers(Ingestion i, Log l,
      TypeToExtensionMapper mapper) {
    Logger log = new SLF4JFromMavenLogger(l);
    List<DefaultIBDataSourceSupplier> k = i.getDataSet().getStreams().stream().map(v -> {
      return new DefaultIBDataSourceSupplier(v.getTemporaryId(),
          new DefaultIBDataSource(log, cet.withReturningTranslation(() -> v.getURL().get()),

              ofNullable(v.getChecksum()), of(v.getMetadata()), ofNullable(v.getMimeType()),
              mapper));
    }).collect(Collectors.toList());
    return new TreeMap<>(k.stream().collect(Collectors.toMap(key -> key.getId(), Function.identity())));

  }

  @SuppressWarnings("unchecked")
  public IBChecksumPathType ingest(Ingestion ingest) throws MojoFailureException {

    Objects.requireNonNull(ingest);

    MavenProject p = getProject().orElseThrow(() -> new MojoFailureException("No supplied project"));

    ingest.getDataSet().injectGAV(p.getGroupId(), p.getArtifactId(), p.getVersion()); // Ugh...side effects
    IBDataSetFinalizer<Ingestion> finalizer;
    try {
      finalizer = (IBDataSetFinalizer<Ingestion>) getDataSetFinalizerSupplier(ingest.getFinalizer(),
          ingest.getFinalizerConfig());
    } catch (ClassCastException e) {
      throw new MojoFailureException("Finalizer " + ingest.getFinalizer() + " was not considered viable", e);
    }
    IBDataIngesterSupplier i = ofNullable(requireNonNull(ingest).getIngester())
        .flatMap(j -> ofNullable(this.allIngesters.get(j)))
        .orElseThrow(() -> new MojoFailureException("No ingester named " + ingest.getIngester()));
    List<IBDataStreamSupplier> suppliers = i
        // Get a new instance of the ingester supplier from configuration
        .config(getConfigMapSupplier())
        // Get the instance
        .get()
        // do the ingestion
        .ingest(ingest.getDataSet(), mapIngestionToSourceSuppliers(ingest, getLog(), getTypeToExtensionMapper()));

    // Ingestion self-returns the IBChecksumPathType, because that's easier and ingestion is easy.

    try {
      return finalizer.finalize(null, ingest, suppliers);
    } catch (IOException e) {
      throw new MojoFailureException("Failed to finalize", e);
    }

  }

}
