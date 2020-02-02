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
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.infrastructurebuilder.IBConstants.IBDATA_PREFIX;
import static org.infrastructurebuilder.IBConstants.IBDATA_SUFFIX;
import static org.infrastructurebuilder.data.IBDataException.cet;
import static org.infrastructurebuilder.util.files.DefaultIBResource.copyToDeletedOnExitTempChecksumAndPath;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.infrastructurebuilder.data.AbstractIBDataSource;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBDataSource;
import org.infrastructurebuilder.data.IBDataSourceSupplier;
import org.infrastructurebuilder.data.IBDataStreamIdentifier;
import org.infrastructurebuilder.data.Metadata;
import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.URLAndCreds;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.IBRuntimeUtils;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.IBResource;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;

@Named
public class DefaultIBDataSourceSupplierMapper extends AbstractIBDataSourceSupplierMapper<String> {
  public final static List<String> HEADERS = Arrays.asList("http://", "https://", "file:", "zip:", "jar:");
  private final WGetterSupplier wgs;
  private final ArchiverManager archiverManager;

  @Inject
  public DefaultIBDataSourceSupplierMapper(IBRuntimeUtils ibr, WGetterSupplier wgs, ArchiverManager am) {
    super(requireNonNull(ibr));
    this.wgs = requireNonNull(wgs);
    this.archiverManager = requireNonNull(am);
  }

  @Override
  public List<String> getHeaders() {
    return HEADERS;
  }

  @Override
  public IBDataSourceSupplier<String> getSupplierFor(IBDataStreamIdentifier v) {
    String temporaryId = v.getTemporaryId().orElse(null);
    URLAndCreds jdbcURL = v.getURLAndCreds().orElseThrow(() -> new IBDataException("No url for " + temporaryId));
    return new DefaultIBDataSourceSupplier<String>(
        // Temp id for this
        temporaryId,
        // datasource
        new DefaultIBDataSource(
            getRuntimeUtils()
            // URL
            , jdbcURL
            // Expand archives
            , v.isExpandArchives()
            // namespace
            , Optional.empty() // FIXME Get a namespace somehow
            // Name
            , v.getName()
            // desc
            , v.getDescription()
            // checksum
            , ofNullable(v.getChecksum())
            // metadata
            , of(v.getMetadata())
            // mime
            , ofNullable(v.getMimeType())
            // wgetter supplier
            , wgs
            // archiver manager (for expanding archives)
            , this.archiverManager
            // Mapper
            , getMapper()),
        getWorkingPathSupplier(), getCredentialsFactory().getCredentialsFor(jdbcURL));
  }

  public class DefaultIBDataSource extends AbstractIBDataSource<String> {
    private final Optional<String> mimeType;
    private final WGetterSupplier wgs;
    private final ArchiverManager am;
    private final TypeToExtensionMapper mapper;
    private List<IBResource> read;

    private DefaultIBDataSource(IBRuntimeUtils ibr, String id, URLAndCreds sourceUrl, boolean expandArchives,
        Optional<Checksum> checksum, Optional<Metadata> metadata, Optional<ConfigMap> additionalConfig,
        Optional<String> namespace, Optional<String> name, Optional<String> description, Optional<String> mimeType,
        WGetterSupplier wgs, ArchiverManager am, TypeToExtensionMapper mapper) {

      super(ibr, id, sourceUrl, expandArchives, namespace, name, description, checksum, metadata, additionalConfig,
          (String) null);
      this.mimeType = mimeType;
      this.wgs = requireNonNull(wgs);
      this.am = requireNonNull(am);
      this.mapper = requireNonNull(mapper);
    }

    public DefaultIBDataSource(IBRuntimeUtils ibr, URLAndCreds source, boolean expandArchives,
        Optional<String> namespace, Optional<String> name, Optional<String> description, Optional<Checksum> checksum,
        Optional<Metadata> metadata, Optional<String> targetType, WGetterSupplier wgs, ArchiverManager am,
        TypeToExtensionMapper mapper) {
      this(ibr, randomUUID().toString(), source, expandArchives, checksum, metadata, empty(), namespace, name,
          description, targetType, wgs, am, mapper);
    }

    @Override
    public IBDataSource<String> configure(ConfigMap config) {
      return new DefaultIBDataSource(getRuntimeUtils(), getId(), getSource(), isExpandArchives(), getChecksum(),
          getMetadata(), of(config), getNamespace(), getName(), getDescription(), getMimeType(), this.wgs, this.am,
          this.mapper);
    }

    @Override
    public Optional<String> getMimeType() {
      return this.mimeType;
    }

    @Override
    public List<IBResource> getInstance(IBRuntimeUtils ibr, String in) {
      Path targetPath = getWorkingPathSupplier().get();
      return ofNullable(targetPath).map(target -> {
        if (this.read == null) {
          List<IBResource> localRead;
          URL src = IBUtils.translateToWorkableArchiveURL(source.getUrl());
          WGetter wget = wgs.get();
          switch (src.getProtocol()) {
          case "http":
          case "https":
            localRead = wget.collectCacheAndCopyToChecksumNamedFile(false,
                // Creds
                getCredentialsFactory().getCredentialsFor(source),
                // Target directory
                targetPath,
                // URL
                src.toExternalForm(),
                // "Optional" checksum
                checksum,
                // Mime type from supplied value (not calculated value)
                this.mimeType,
                // Slightly insane defaults
                3, 0, false, isExpandArchives()).orElseThrow(() -> new IBDataException("Could not read "));
            break;
          case "file":
          case "zip":
          case "jar":
            try (InputStream ins = src.openStream()) {
              localRead = new ArrayList<>();
              IBResource val = cet.withReturningTranslation(
                  () -> copyToDeletedOnExitTempChecksumAndPath(targetPath, IBDATA_PREFIX, IBDATA_SUFFIX, ins));
              localRead.add(val);
              if (isExpandArchives()) {
                localRead.addAll(wget.expand(targetPath, val, of(src.toExternalForm())));
              }

            } catch (IOException e) {
              throw new IBDataException(e);
            }
            break;
          default:
            throw new IBDataException("Default processor cannot handle protocol " + source);
          }
          read = localRead;
        }
        return read;
      }).orElse(Collections.emptyList());
    }

  }

}
