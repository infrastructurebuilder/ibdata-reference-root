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

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.infrastructurebuilder.data.IBDataException.cet;
import static org.infrastructurebuilder.data.IBMetadataUtils.MAP_SPLITTER;
import static org.infrastructurebuilder.data.IBMetadataUtils.RECORD_SPLITTER;
import static org.infrastructurebuilder.data.IBMetadataUtils.TRANSFORMERSLIST;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.infrastructurebuilder.IBConstants;
import org.infrastructurebuilder.data.DefaultIBDataSet;
import org.infrastructurebuilder.data.DefaultIBDataTransformationResult;
import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBDataSet;
import org.infrastructurebuilder.data.IBDataStream;
import org.infrastructurebuilder.data.IBDataStreamRecordFinalizer;
import org.infrastructurebuilder.data.IBDataStreamSupplier;
import org.infrastructurebuilder.data.IBDataTransformationError;
import org.infrastructurebuilder.data.IBDataTransformationResult;
import org.infrastructurebuilder.data.model.DataStream;
import org.infrastructurebuilder.data.transform.AbstractIBDataTransformer;
import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.artifacts.Checksum;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.DefaultConfigMapSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class AbstractIBDataRecordBasedTransformer extends AbstractIBDataTransformer {
  public final static Logger log = LoggerFactory.getLogger(AbstractIBDataRecordBasedTransformer.class);
  private final Map<String, IBDataRecordTransformerSupplier> dataLineSuppliers;
  private final List<IBDataRecordTransformer<?, ?>> configuredTranformers;
  private final IBDataStreamRecordFinalizer configuredFinalizer;
  private final Optional<String> finalType;

  protected AbstractIBDataRecordBasedTransformer(Path workingPath, Logger log, ConfigMap config,
      Map<String, IBDataRecordTransformerSupplier> dataRecTransformerSuppliers, IBDataStreamRecordFinalizer finalizer) {
    super(workingPath, log, config);
    this.dataLineSuppliers = dataRecTransformerSuppliers;
    this.configuredFinalizer = finalizer;
    if (config != null && config.containsKey(TRANSFORMERSLIST)
    /*&& config.containsKey(UNCONFIGURABLEKEY_FINALIZER_KEY)*/) {
      Map<String, IBDataRecordTransformerSupplier<?, ?>> map = new HashMap<>();
      this.dataLineSuppliers.forEach((k, v) -> {
        map.put(k, v);
      });
      //      this.failOnError = ofNullable(config.get(FAIL_ON_ERROR_KEY)).map(Boolean::parseBoolean).orElse(false);
      ConfigMapSupplier lcfg = new DefaultConfigMapSupplier().addConfiguration(config);
      // If the map contains the key, then the suppliers MUST contain all indicated line transformers
      String theListString = config.get(TRANSFORMERSLIST);
      List<String> theList = Arrays.asList(theListString.split(Pattern.quote(RECORD_SPLITTER)));
      if (theList.size() > 0) {
        Map<String, String> idToHint = theList.stream().map(s -> s.split(Pattern.quote(MAP_SPLITTER)))
            .collect(Collectors.toMap(k -> k[0], v -> v[1]));
        ArrayList<String> tList = new ArrayList<>(idToHint.values());
        tList.removeAll(dataRecTransformerSuppliers.keySet());
        if (tList.size() > 0)
          throw new IBDataException("Missing record transformers " + tList + "\n Available transformers are "
              + dataRecTransformerSuppliers.keySet());
        configuredTranformers = new ArrayList<>();
        Optional<String> previousType = Optional.empty();
        for (String li : theList) {
          String[] s = li.split(Pattern.quote(MAP_SPLITTER));
          IBDataRecordTransformerSupplier<?, ?> s2 = map.get(s[1]).configure(lcfg);
          IBDataRecordTransformer<?, ?> transformer = s2.get().configure(config);
          if (acceptable(previousType, transformer.accepts()))
            previousType = transformer.produces();
          else
            throw new IBDataException("Transformer " + s[0] + "/" + transformer.getHint() + " only responds to "
                + transformer.accepts().get() + " and it's predecessor produces " + previousType.orElse("unknown"));
          configuredTranformers.add(transformer);
        }
        this.finalType = previousType;
      } else {
        this.configuredTranformers = null;
        this.finalType = Optional.empty();
      }
    } else {
      this.configuredTranformers = null;
      this.finalType = Optional.empty();
    }
  }

  private boolean acceptable(Optional<String> previous, Optional<List<String>> optional) {
    return (!previous.isPresent() || !optional.isPresent() || optional.get().contains(previous.get()));
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public IBDataTransformationResult transform(IBDataSet ds, List<IBDataStream> suppliedStreams, boolean failOnError) {
    IBDataStreamRecordFinalizer cf = getConfiguredFinalizer();
    if (!acceptable(finalType, cf.accepts()))
      throw new IBDataException(
          "Finalizer " + cf.getId() + " does not accept finally produced type " + finalType.get());
    return localTransform(ds, suppliedStreams, getConfiguredFinalizer(), failOnError);
  }

  public List<IBDataRecordTransformer<?, ?>> getConfiguredTransformers() {
    return ofNullable(configuredTranformers)
        .orElseThrow(() -> new IBDataException("No list of configured record transformers"));
  }

  protected Map<String, IBDataRecordTransformerSupplier> getDataLineSuppliers() {
    return dataLineSuppliers;
  }

  protected IBDataStreamRecordFinalizer getConfiguredFinalizer() {
    return configuredFinalizer;
  }

  private Stream<String> streamFor(IBDataStreamSupplier ibds) {
    try (InputStream ins = Objects.requireNonNull(ibds).get().get()) {
      return IBUtils.readInputStreamAsStringStream(ins);
    } catch (IOException e) {
      throw new IBDataException(e);
    }
  }

  // This is a LITTLE bit dangerous
  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected String processStream(IBDataStream stream, IBDataStreamRecordFinalizer finalizer,
      Map<String, List<Long>> errors, List<IBDataTransformationError> errorList) {
    return cet.withReturningTranslation(() -> {
      String inbound;
      try (BufferedReader r = new BufferedReader(new InputStreamReader(stream.get()))) {
        String line;
        inbound = String.class.getCanonicalName();
        Optional<Object> inboundObject = empty(), s;
        long lineCount = 0;
        while ((line = cet.withReturningTranslation(() -> r.readLine())) != null) {
          lineCount++;
          //              log.info(String.format("Line %05d '%s'", lineCount, line));
          s = of(line);
          for (IBDataRecordTransformer t : getConfiguredTransformers()) {

            inboundObject = ofNullable(t.apply(s.get()));
            //                log.info(String.format("           '%s'",  line));
            if (!inboundObject.isPresent()) {
              errors.computeIfAbsent(t.getHint(), k -> new ArrayList<Long>()).add(lineCount);
              break;
            }
            s = inboundObject;
            inbound = (String) t.produces().orElse(inbound);
          }

          inboundObject.ifPresent(l -> {
            //                log.info(String.format("        as '%s'", l));
            finalizer.writeRecord(l).ifPresent(e -> errorList.add((IBDataTransformationError) e));
          });

        }
      }
      return (String) finalizer.produces().orElse(stream.getMimeType());
    });
  }

  protected IBDataTransformationResult localTransform(IBDataSet ds2, List<IBDataStream> suppliedStreams,
      IBDataStreamRecordFinalizer finalizer, boolean failOnError) {
    requireNonNull(finalizer, "No finalizer supplied to localTransform");
    final Map<String, List<Long>> errors = new HashMap<>();
    final List<IBDataTransformationError> errorList = new ArrayList<>();
    Map<UUID, IBDataStreamSupplier> map = new HashMap<>();

    String finalType = null;
    for (IBDataStream stream : Stream
        .concat(requireNonNull(ds2, "Supplied transform dataset").asStreamsList().stream(), suppliedStreams.stream())
        .collect(Collectors.toList())) {
      if (this.respondsTo(stream)) {
        finalType = processStream(stream, finalizer, errors, errorList);
      }
    }
    cet.withTranslation(() -> finalizer.close());
    Path targetPath = finalizer.getWorkingPath();
    Checksum c = new Checksum(targetPath);
    ds2.getStreamSuppliers().forEach(ss -> map.put(ss.getId(), ss));
    DataStream newStream = new DataStream();
    newStream.setMimeType(Optional.ofNullable(finalType).orElse(IBConstants.APPLICATION_OCTET_STREAM));
    newStream.setMetadata(new Xpp3Dom("metadata"));
    newStream.setCreationDate(new Date());
    newStream.setUuid(c.asUUID().get().toString());
    newStream.setSourceURL(cet.withReturningTranslation(() -> targetPath.toUri().toURL().toExternalForm()));
    newStream.setSha512(c.toString());
    IBDataStreamSupplier x = finalizer.finalizeRecord(newStream);
    map.put(x.getId(), x);
    IBDataSet newSet = new DefaultIBDataSet(ds2).withStreamSuppliers(map);
    return new DefaultIBDataTransformationResult(ofNullable(newSet), errorList);
  }

}
