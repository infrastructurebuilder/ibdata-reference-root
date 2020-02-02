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

import static org.infrastructurebuilder.IBConstants.FAIL_ON_ERROR;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.data.IBSchema;
import org.infrastructurebuilder.data.transform.IBDataIntermediary;
import org.infrastructurebuilder.data.transform.IBSchemaOutboundRowMapper;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.jooq.tools.csv.usurped.CSVParser;

@Named("csv")
public class DefaultCSVIBSchemaOutboundRowMapper extends AbstractRowMapper implements IBSchemaOutboundRowMapper<String> {

  private final CSVParser parser;
  private final Map<Integer, String> nameMap;

  @Inject
  public DefaultCSVIBSchemaOutboundRowMapper() {
    this(null, true, new CSVParser(), Collections.emptyMap());
  }

  private DefaultCSVIBSchemaOutboundRowMapper(IBSchema in, boolean failOnError, CSVParser p,
      Map<Integer, String> nameMap) {
    super(in, failOnError);
    this.parser = p;
    this.nameMap = Objects.requireNonNull(nameMap);
  }

  @Override
  public String getOutboundType() {
    return String.class.getCanonicalName();
  }

  protected char getSeparator() {
    return CSVParser.DEFAULT_SEPARATOR;
  }

  @Override
  public IBSchemaOutboundRowMapper<String> configure(IBSchema inboundTargetSchema, ConfigMap cm) {
    boolean foe = cm.getParsedBoolean(FAIL_ON_ERROR, true);
    Map<Integer, String> nMap = cm.get("nameMap");
    char quotechar = cm.getOptionalString("quoteChar").map(s -> s.charAt(0)).orElse(CSVParser.DEFAULT_QUOTE_CHARACTER);
    char escape = cm.getOptionalString("escape").map(s -> s.charAt(0)).orElse(CSVParser.DEFAULT_ESCAPE_CHARACTER);
    boolean ignoreLeadingWhiteSpace = cm.getParsedBoolean("ignoreLeadingWhiteSpace",
        CSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE);
    boolean strictQuotes = cm.getParsedBoolean("strictQuotes", CSVParser.DEFAULT_STRICT_QUOTES);
    CSVParser c = new CSVParser(getSeparator(), quotechar, escape, strictQuotes, ignoreLeadingWhiteSpace);
    return new DefaultCSVIBSchemaOutboundRowMapper(inboundTargetSchema, foe, c, nMap);
  }

  @Override
  public Optional<String> map(IBDataIntermediary row) {

    final StringJoiner sj = new StringJoiner(Character.toString(getSeparator()));
    getSchema().getSchemaFields().stream().forEach(sf -> {
      if (!sf.isDeprecated()) {
        sj.add("\"" + row.get(sf.getName()).toString() + "\"");
      }
    });

    return Optional.of(sj.toString());
  }

}
