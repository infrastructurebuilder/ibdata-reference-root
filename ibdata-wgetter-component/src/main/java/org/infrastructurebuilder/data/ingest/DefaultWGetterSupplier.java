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
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.infrastructurebuilder.IBConstants.APPLICATION_OCTET_STREAM;
import static org.infrastructurebuilder.IBConstants.IBDATA_PREFIX;
import static org.infrastructurebuilder.IBConstants.IBDATA_SUFFIX;
import static org.infrastructurebuilder.data.IBDataConstants.*;
import static org.infrastructurebuilder.data.IBDataException.cet;
import static org.infrastructurebuilder.util.files.DefaultIBChecksumPathType.*;

import java.io.File;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.apache.http.ssl.SSLContexts;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.bzip2.BZip2UnArchiver;
import org.codehaus.plexus.archiver.gzip.GZipUnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.snappy.SnappyUnArchiver;
import org.codehaus.plexus.archiver.xz.XZUnArchiver;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.StringUtils;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.util.BasicCredentials;
import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.LoggerSupplier;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.PathSupplier;
import org.infrastructurebuilder.util.files.BasicIBChecksumPathType;
import org.infrastructurebuilder.util.files.DefaultIBChecksumPathType;
import org.infrastructurebuilder.util.files.IBChecksumPathType;
import org.infrastructurebuilder.util.files.TypeToExtensionMapper;
import org.slf4j.Logger;

import com.googlecode.download.maven.plugin.internal.HttpFileRequester;
import com.googlecode.download.maven.plugin.internal.LoggingProgressReport;
import com.googlecode.download.maven.plugin.internal.SignatureUtils;
import com.googlecode.download.maven.plugin.internal.SilentProgressReport;
import com.googlecode.download.maven.plugin.internal.cache.DownloadCache;

@Named
public class DefaultWGetterSupplier implements WGetterSupplier {

  private final Logger log;
  private final TypeToExtensionMapper t2e;
  private final PathSupplier cacheDirectory;
  private final ArchiverManager archiverManager;
  private final PathSupplier workingDirectory;

  @Inject
  public DefaultWGetterSupplier(LoggerSupplier log, TypeToExtensionMapper t2e,
      @Named(IBDATA_WORKING_PATH_SUPPLIER) PathSupplier workingPathSupplier,
      @Named(IBDATA_DOWNLOAD_CACHE_DIR_SUPPLIER) PathSupplier cacheDirSupplier, ArchiverManager archiverManager) {
    this.log = requireNonNull(log).get();
    this.t2e = requireNonNull(t2e);
    this.cacheDirectory = requireNonNull(cacheDirSupplier);
    this.archiverManager = requireNonNull(archiverManager);
    this.workingDirectory = requireNonNull(workingPathSupplier);
  }

  @Override
  public WGetter get() {
    return new DefaultWGetter(log, t2e, cacheDirectory.get(), workingDirectory.get(), this.archiverManager);
  }

  /*
   *
   * Encapsulates code from download-maven-plugin
   */
  private class DefaultWGetter implements WGetter {

    private final WGet wget;
    private final Log log;
    private final ArchiverManager am;
    private final Path workingDir;

    public DefaultWGetter(Logger log, TypeToExtensionMapper t2e, Path cacheDir, Path workingDir,
        ArchiverManager archiverManager) {
      // this.wps = requireNonNull(wps);
      this.wget = new WGet();
      // FIXME Add dep on version > 0.10.0 of iblogging-maven-component and then
      // create a new LoggingMavenComponent from log
      // Log l2 = new LoggingMavenComponent(log);
//      Logger localLogger = requireNonNull(log); // FIXME (See above)
      Log l2 = new DefaultLog(new ConsoleLogger(0, WGetter.class.getCanonicalName()));
      this.wget.setLog(l2);
      this.wget.setT2EMapper(Objects.requireNonNull(t2e));
      this.wget.setCacheDirectory(requireNonNull(cacheDir).toFile());
      this.log = l2;
      this.am = requireNonNull(archiverManager);
      this.workingDir = requireNonNull(workingDir);
    }

    @Override
    synchronized public final Optional<List<IBChecksumPathType>> collectCacheAndCopyToChecksumNamedFile(
        boolean deleteExistingCacheIfPresent, Optional<BasicCredentials> creds, Path outputPath, String sourceString,
        Optional<Checksum> checksum, Optional<String> type, int retries, int readTimeOut, boolean skipCache,
        boolean expandArchives) {

      wget.setDeleteIfPresent(deleteExistingCacheIfPresent);
      requireNonNull(creds).ifPresent(bc -> {
        wget.setUsername(bc.getKeyId());
        wget.setPassword(bc.getSecret().orElse(null));
      });

      wget.setOutputPath(outputPath);
      requireNonNull(checksum).ifPresent(c -> wget.setSha512(c.toString().toLowerCase()));
      wget.setUri(cet
          .withReturningTranslation(() -> IBUtils.translateToWorkableArchiveURL(requireNonNull(sourceString)).toURI()));
      wget.setFailOnError(false);
      wget.setOverwrite(false);
//      wget.setInteractiveMode(interactiveMode);   // Never
      wget.setRetries(retries);
      wget.setReadTimeOut(readTimeOut);
      wget.setSkipCache(skipCache);
      wget.setCheckSignature(checksum.isPresent());
      wget.setMimeType(type.orElse(null));
      Optional<List<IBChecksumPathType>> o = cet.withReturningTranslation(() -> this.wget.downloadIt());
      if (expandArchives) {
        o.ifPresent(c -> {
          IBChecksumPathType src = c.get(0);
          List<IBChecksumPathType> l = expand(workingDir, src,
              src.getSourceURL().map(URL::toExternalForm).map(n -> "zip:" + n));
          c.addAll(l);
        });
      }
      return o;
    }

    @Override
    public List<IBChecksumPathType> expand(Path tempPath, IBChecksumPathType src, Optional<String> oSource) {

      Path source = requireNonNull(src).getPath();
      List<IBChecksumPathType> l = new ArrayList<>();
      Path targetDir = cet.withReturningTranslation(() -> Files.createTempDirectory(IBDATA_PREFIX)).toAbsolutePath();
      File outputFile = source.toFile();
      File outputDirectory = targetDir.toFile();
      String outputFileName = source.toAbsolutePath().toString();
      try {
        String type = t2e.getExtensionForType(src.getType()).substring(1);
        UnArchiver unarchiver = this.am.getUnArchiver(type);
        log.debug("Unarchiver type is " + type + " " + unarchiver.toString());
        unarchiver.setSourceFile(outputFile);
        if (isFileUnArchiver(unarchiver)) {
          unarchiver
              .setDestFile(new File(outputDirectory, outputFileName.substring(0, outputFileName.lastIndexOf('.'))));
        } else {
          unarchiver.setDestDirectory(outputDirectory);
        }
        unarchiver.extract();
        String rPath = cet.withReturningTranslation(() -> targetDir.toUri().toURL().toExternalForm());
        for (Path p : IBUtils.allFilesInTree(targetDir)) {
          String tPath = cet.withReturningTranslation(() -> p.toUri().toURL().toExternalForm())
              .substring(rPath.length());
          IBChecksumPathType q = cet
              .withReturningTranslation(() -> copyToTempChecksumAndPath(tempPath, p, oSource, tPath));
          l.add(q);
        }

        IBUtils.deletePath(targetDir);
      } catch (NoSuchArchiverException e) {
        // File has no archiver because reasons, but that's OK
        log.debug("File " + outputFile + " has no available archiver");
      }
      return l;
    }

    private boolean isFileUnArchiver(final UnArchiver unarchiver) {
      return unarchiver instanceof BZip2UnArchiver || unarchiver instanceof GZipUnArchiver
          || unarchiver instanceof SnappyUnArchiver || unarchiver instanceof XZUnArchiver;
    }

  }

  /**
   * Will download a file from a web site using the standard HTTP protocol.
   *
   * @author Marc-Andre Houle
   * @author Mickael Istria (Red Hat Inc)
   */
  private static class WGet {

    private static final PoolingHttpClientConnectionManager CONN_POOL;

    static {
      CONN_POOL = new PoolingHttpClientConnectionManager(
          RegistryBuilder.<ConnectionSocketFactory>create()
              .register("http", PlainConnectionSocketFactory.getSocketFactory())
              .register("https",
                  new SSLConnectionSocketFactory(SSLContexts.createSystemDefault(),
                      new String[] { "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2" }, null,
                      SSLConnectionSocketFactory.getDefaultHostnameVerifier()))
              .build(),
          null, null, null, 1, TimeUnit.MINUTES);
    }

    /**
     * Represent the URL to fetch information from.
     */
    // @Parameter(alias = "url", property = "download.url", required = true)
    private URI uri;

    /**
     * Flag to overwrite the file by redownloading it
     */
    // @Parameter(property = "download.overwrite")
    private boolean overwrite;

    /**
     * Represent the file name to use as output value. If not set, will use last
     * segment of "url"
     */
    // @Parameter(property = "download.outputFileName")
    // private String outputFileName;

    /**
     * Represent the directory where the file should be downloaded.
     */
    // @Parameter(property = "download.outputDirectory", defaultValue =
    // "${project.build.directory}", required = true)
    // private Path outputDirectory;
    private Path outputPath;

    /**
     * The md5 of the file. If set, file signature will be compared to this
     * signature and plugin will fail.
     */
    // @Parameter
    // private String md5;

    /**
     * The sha1 of the file. If set, file signature will be compared to this
     * signature and plugin will fail.
     */
    // @Parameter
    // private String sha1;

    /**
     * The sha512 of the file. If set, file signature will be compared to this
     * signature and plugin will fail.
     */
    // @Parameter
    private String sha512;

    // /**
    // * Whether to unpack the file in case it is an archive (.zip)
    // */
    //// @Parameter(property = "download.unpack", defaultValue = "false")
    // private boolean unpack;

    /**
     * Server Id from settings file to use for authentication Only one of serverId
     * or (username/password) may be supplied
     */
    // @Parameter
    // private String serverId;

    /**
     * Custom username for the download
     */
    // @Parameter
    private String username;

    /**
     * Custom password for the download
     */
    // @Parameter
    private String password;

    /**
     * How many retries for a download
     */
    // @Parameter(defaultValue = "2")
    private int retries;

    /**
     * Read timeout for a download in milliseconds
     */
    // @Parameter(defaultValue = "0")
    private int readTimeOut;

    /**
     * Download file without polling cache
     */
    // @Parameter(property = "download.cache.skip", defaultValue = "false")
    private boolean skipCache;

    /**
     * The directory to use as a cache. Default is
     * ${local-repo}/.cache/maven-download-plugin
     */
    // @Parameter(property = "download.cache.directory")
    private File cacheDirectory;

    /**
     * Flag to determine whether to fail on an unsuccessful download.
     */
    // @Parameter(defaultValue = "true")
    private boolean failOnError;

    /**
     * Whether to skip execution of Mojo
     */
    // @Parameter(property = "download.plugin.skip", defaultValue = "false")
    // private boolean skip;

    /**
     * Whether to check the signature of existing files
     */
    // @Parameter(property = "checkSignature", defaultValue = "false")
    private boolean checkSignature;

    // @Parameter(property = "session")
    // private MavenSession session;

    // @Component
    // private ArchiverManager archiverManager;

    /**
     * For transfers
     */
    // @Component
    // private WagonManager wagonManager;

    // @Component
    // private BuildContext buildContext;

    // @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    // private Settings settings;

    // /**
    // * Maven Security Dispatcher
    // */
    // @Component( hint = "mng-4384" )
    // private SecDispatcher securityDispatcher;

    /** Instance logger */
    // @Component
    private Log log;

    private boolean deleteIfPresent = false;

    public Log getLog() {
      return log;
    }

    public void setDeleteIfPresent(boolean deleteExistingCacheIfPresent) {
      this.deleteIfPresent = deleteExistingCacheIfPresent;
    }

    /**
     * Method call whent he mojo is executed for the first time.
     *
     * @throws MojoExecutionException if an error is occuring in this mojo.
     * @throws MojoFailureException   if an error is occuring in this mojo.
     */
    // @Override
    // public void execute() throws MojoExecutionException, MojoFailureException {
    // if (this.skip) {
    // getLog().info("maven-download-plugin:wget skipped");
    // return;
    // }
    // NOTE FYI: Always returns a list of size() == 1 or empty()
    public Optional<List<IBChecksumPathType>> downloadIt() throws MojoExecutionException, MojoFailureException {
//      if (/*StringUtils.isNotBlank(serverId) && */ (StringUtils.isNotBlank(username)
//          || StringUtils.isNotBlank(password))) {
//        throw new MojoExecutionException("Specify either serverId or username/password, not both");
//      }

      // if (settings == null) {
      // getLog().warn("settings is null");
      // }
      // getLog().debug("Got settings");
      if (retries < 1) {
        throw new MojoFailureException("retries must be at least 1");
      }

      // PREPARE
      // if (this.outputFileName == null) {
      // try {
      // this.outputFileName = new File(this.uri.toURL().getFile()).getName();
      // } catch (Exception ex) {
      // throw new MojoExecutionException("Invalid URL", ex);
      // }
      // }
      // if (this.cacheDirectory == null) {
      // this.cacheDirectory = new File(this.session.getLocalRepository()
      // .getBasedir(), ".cache/download-maven-plugin");
      // }
      getLog().debug("Cache is: " + this.cacheDirectory.getAbsolutePath());
      DownloadCache cache = new DownloadCache(this.cacheDirectory);
      Path outputDirectory = outputPath;
      String outputFileName = UUID.randomUUID().toString();
      IBDataException.cet.withTranslation(() -> Files.createDirectories(outputDirectory));
      // this.outputDirectory.mkdirs();
      File outputFile = outputDirectory.resolve(outputFileName).toFile();
      // File outputFile = new File(this.outputDirectory, this.outputFileName);

      // DO
      try {

        boolean haveFile = false;
        /*
         * boolean haveFile = outputFile.exists(); // Literally never happens if
         * (haveFile) { boolean signatureMatch = true; if (this.checkSignature) { String
         * expectedDigest = null, algorithm = null; // if (this.md5 != null) { //
         * expectedDigest = this.md5; // algorithm = "MD5"; // } // // if (this.sha1 !=
         * null) { // expectedDigest = this.sha1; // algorithm = "SHA1"; // }
         *
         * if (this.sha512 != null) { expectedDigest = this.sha512; algorithm =
         * "SHA-512"; }
         *
         * if (expectedDigest != null) { try {
         * SignatureUtils.verifySignature(outputFile, expectedDigest,
         * MessageDigest.getInstance(algorithm)); } catch (MojoFailureException e) {
         * getLog().warn( "The local version of file " + outputFile.getName() +
         * " doesn't match the expected signature. " +
         * "You should consider checking the specified signature is correctly set.");
         * signatureMatch = false; } } }
         *
         * // TODO verify last modification date if (!signatureMatch) {
         * outputFile.delete(); haveFile = false; } else if (!overwrite) {
         * getLog().info("File already exist, skipping"); } else { // If no signature
         * provided and owerwriting requested we // will treat the fact as if there is
         * no file in the cache. haveFile = false; } }
         */
        Checksum finalChecksum;
//        if (!haveFile) {
        File cached = cache.getArtifact(this.uri, null /* this.md5 */, null /* this.sha1 */, this.sha512);
        if (this.deleteIfPresent && cached != null && cached.exists()) {
          getLog().warn("Deleting cached file " + cached);
          cached.delete();
        }
        if (!this.skipCache && cached != null && cached.exists()) {
          getLog().info("Got from cache: " + cached.getAbsolutePath());
          Files.copy(cached.toPath(), outputFile.toPath());
        } else {
          boolean done = false;
          while (!done && this.retries > 0) {
            try {
              getLog().debug("Getting " + this.uri.toASCIIString() + " to file " + outputFile );
              doGet(outputFile);
              // if (this.md5 != null) {
              // SignatureUtils.verifySignature(outputFile, this.md5,
              // MessageDigest.getInstance("MD5"));
              // }
              // if (this.sha1 != null) {
              // SignatureUtils.verifySignature(outputFile, this.sha1,
              // MessageDigest.getInstance("SHA1"));
              // }
              if (this.sha512 != null) {
                SignatureUtils.verifySignature(outputFile, this.sha512, MessageDigest.getInstance("SHA-512"));
              }
              done = true;
            } catch (Exception ex) {
              getLog().warn("Could not get content", ex);
              this.retries--;
              if (this.retries > 0) {
                getLog().warn("Retrying (" + this.retries + " more)");
              }
            }
          }
          if (!done) {
            if (failOnError) {
              throw new MojoFailureException("Could not get content");
            } else {
              getLog().warn("Not failing download despite download failure.");
              // return;
              return empty();
            }
          }
        }
//        }
        finalChecksum = (this.sha512 == null ? new Checksum(outputFile.toPath()) : new Checksum(this.sha512));
        DefaultIBChecksumPathType pVal = new DefaultIBChecksumPathType(outputFile.toPath(), finalChecksum, empty());
        String computedType = pVal.getType();
        if (this.mimeType == null)
          this.mimeType = computedType;
        cache.install(this.uri, outputFile, null /* this.md5 */, null /* this.sha1 */, finalChecksum.toString());
        /* Get the "final name" */
        String finalFileName = finalChecksum.asUUID().get().toString() + t2e.getExtensionForType(this.mimeType);
        Path newTarget = outputPath.resolve(finalFileName);
        try {
          IBUtils.moveAtomic(outputFile.toPath(), newTarget);
        } finally {
          outputFile.delete();
        }

        // if (this.unpack) {
        // unpack(outputFile);
        //// buildContext.refresh(outputDirectory);
        // return Optional.of(outputDirectory.toPath());
        // } else {
        //// buildContext.refresh(outputFile);
        Path outPath = newTarget;

        DefaultIBChecksumPathType retVal = new DefaultIBChecksumPathType(outPath, finalChecksum,
            ofNullable(this.mimeType));
        retVal.setSource(this.uri.toURL().toExternalForm());
        List<IBChecksumPathType> retVall = new ArrayList<>();
        retVall.add(retVal);
        return Optional.of((retVall));
        // }
      } catch (Exception ex) {
        throw new MojoExecutionException("IO Error", ex);
      }
    }

    // private void unpack(File outputFile) throws NoSuchArchiverException {
    // UnArchiver unarchiver = this.archiverManager.getUnArchiver(outputFile);
    // unarchiver.setSourceFile(outputFile);
    // if (isFileUnArchiver(unarchiver)) {
    // unarchiver.setDestFile(new File(this.outputDirectory,
    // outputFileName.substring(0, outputFileName.lastIndexOf('.'))));
    // } else {
    // unarchiver.setDestDirectory(this.outputDirectory);
    // }
    // unarchiver.extract();
    // outputFile.delete();
    // }
    //
    // private boolean isFileUnArchiver(final UnArchiver unarchiver) {
    // return unarchiver instanceof BZip2UnArchiver ||
    // unarchiver instanceof GZipUnArchiver ||
    // unarchiver instanceof SnappyUnArchiver ||
    // unarchiver instanceof XZUnArchiver;
    // }
    //

    private void doGet(final File outputFile) throws Exception {
      final RequestConfig requestConfig;
      if (readTimeOut > 0) {
        getLog().info("Read Timeout is set to " + readTimeOut + " milliseconds (apprx "
            + Math.round(readTimeOut * 1.66667e-5) + " minutes)");
        requestConfig = RequestConfig.custom().setConnectTimeout(readTimeOut).setSocketTimeout(readTimeOut).build();
      } else {
        requestConfig = RequestConfig.DEFAULT;
      }

      CredentialsProvider credentialsProvider = null;
      if (StringUtils.isNotBlank(username)) {
        getLog().debug("providing custom authentication");
        getLog().debug("username: " + username + " and password: ***");

        credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(this.uri.getHost(), this.uri.getPort()),
            new UsernamePasswordCredentials(username, password));

        // } else if (StringUtils.isNotBlank(serverId)) {
        // getLog().debug("providing custom authentication for " + serverId);
        // Server server = settings.getServer(serverId);
        // if (server == null) {
        // throw new MojoExecutionException(String.format("Server %s not found",
        // serverId));
        // }
        // getLog().debug(String.format("serverId %s supplies username: %s and password:
        // ***", serverId, server.getUsername() ));
        //
        // credentialsProvider = new BasicCredentialsProvider();
        // credentialsProvider.setCredentials(
        // new AuthScope(this.uri.getHost(), this.uri.getPort()),
        // new UsernamePasswordCredentials(server.getUsername(),
        // decrypt(server.getPassword(), serverId)));

      }

      final HttpRoutePlanner routePlanner;
      // ProxyInfo proxyInfo = this.wagonManager.getProxy(this.uri.getScheme());
      // ProxyInfo proxyInfo = (ProxyInfo) this.proxyInfoSupplier.get();
      if (proxyInfo != null && proxyInfo.getHost() != null && ProxyInfo.PROXY_HTTP.equals(proxyInfo.getType())) {
        routePlanner = new DefaultProxyRoutePlanner(new HttpHost(proxyInfo.getHost(), proxyInfo.getPort()));
        if (proxyInfo.getUserName() != null) {
          final Credentials creds;
          if (proxyInfo.getNtlmHost() != null || proxyInfo.getNtlmDomain() != null) {
            creds = new NTCredentials(proxyInfo.getUserName(), proxyInfo.getPassword(), proxyInfo.getNtlmHost(),
                proxyInfo.getNtlmDomain());
          } else {
            creds = new UsernamePasswordCredentials(proxyInfo.getUserName(), proxyInfo.getPassword());
          }
          AuthScope authScope = new AuthScope(proxyInfo.getHost(), proxyInfo.getPort());
          if (credentialsProvider == null) {
            credentialsProvider = new BasicCredentialsProvider();
          }
          credentialsProvider.setCredentials(authScope, creds);
        }
      } else {
        routePlanner = new SystemDefaultRoutePlanner(ProxySelector.getDefault());
      }

      final CloseableHttpClient httpClient = HttpClientBuilder.create().setConnectionManager(CONN_POOL)
          .setConnectionManagerShared(true).setRoutePlanner(routePlanner).build();

      final HttpFileRequester fileRequester = new HttpFileRequester(httpClient,
          /* this.session.getSettings().isInteractiveMode() */

          /*
           * this.interactiveMode ? new LoggingProgressReport(getLog()) :
           */new SilentProgressReport(getLog()));

      final HttpClientContext clientContext = HttpClientContext.create();
      clientContext.setRequestConfig(requestConfig);
      if (credentialsProvider != null) {
        clientContext.setCredentialsProvider(credentialsProvider);
      }

      fileRequester.download(this.uri, outputFile, clientContext);
    }

    // private String decrypt(String str, String server) {
    // try {
    // return securityDispatcher.decrypt(str);
    // }
    // catch(SecDispatcherException e) {
    // getLog().warn(String.format("Failed to decrypt password/passphrase for server
    // %s, using auth token as is", server), e);
    // return str;
    // }
    // }

    // ************************ Adding Constructor and setters for private params so
    // that this is a component

    public void setUri(URI uri) {
      this.uri = uri;
    }

    public void setOverwrite(boolean overwrite) {
      this.overwrite = overwrite;
    }

    // public void setOutputFileName(String outputFileName) {
    // this.outputFileName = outputFileName;
    // }

    public void setOutputPath(Path outputPath) {
      this.outputPath = outputPath;
    }

    public void setSha512(String sha512) {
      this.sha512 = sha512;
    }

    // public void setUnpack(boolean unpack) {
    // this.unpack = unpack;
    // }
    //

    public void setUsername(String username) {
      this.username = username;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public void setRetries(int retries) {
      this.retries = retries;
    }

    public void setReadTimeOut(int readTimeOut) {
      this.readTimeOut = readTimeOut;
    }

    public void setSkipCache(boolean skipCache) {
      this.skipCache = skipCache;
    }

    public void setCacheDirectory(File cacheDirectory) {
      this.cacheDirectory = cacheDirectory;
    }

    public void setFailOnError(boolean failOnError) {
      this.failOnError = failOnError;
    }

    public void setCheckSignature(boolean checkSignature) {
      this.checkSignature = checkSignature;
    }

    private boolean interactiveMode = false;

//    public void setInteractiveMode(boolean interactiveMode) {
//      this.interactiveMode = interactiveMode;
//    }

    private ProxyInfo proxyInfo;

    private String mimeType = null;

    public void setMimeType(String mimeType) {
      this.mimeType = mimeType;
    }

    private TypeToExtensionMapper t2e;

    public void setT2EMapper(TypeToExtensionMapper t2e) {
      this.t2e = t2e;
    }

    public void setLog(Log log) {
      this.log = log;
    }
    /*
     * log, proxyInfoSupplier are required!!!
     */

    // public void setLog(LoggerSupplier log) {
    // this.log = cet.withReturningTranslation(() -> ((Log)
    // requireNonNull(log).get()));
    // }
    //
    // public void setProxySupplier(ProxyInfoSupplier pi) {
    // this.proxyInfo = cet.withReturningTranslation(() -> ((ProxyInfo)
    // requireNonNull(pi).get()));
    // }
  }

}
