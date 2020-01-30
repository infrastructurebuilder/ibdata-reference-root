package org.infrastructurebuilder.data.transform.line;

import static java.util.Optional.ofNullable;
import static org.infrastructurebuilder.IBConstants.FAIL_ON_ERROR;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.infrastructurebuilder.data.IBDataException;
import org.infrastructurebuilder.data.IBField;
import org.infrastructurebuilder.data.IBSchema;
import org.infrastructurebuilder.data.transform.DefaultIBDataIntermediary;
import org.infrastructurebuilder.data.transform.IBDataIntermediary;
import org.infrastructurebuilder.data.transform.IBSchemaInboundRowMapper;
import org.infrastructurebuilder.util.config.ConfigMap;
import org.jooq.tools.csv.usurped.CSVParser;

@Named("csv")
public class DefaultCSVIBSchemaInboundRowMapper extends AbstractRowMapper implements IBSchemaInboundRowMapper<String> {

  private final CSVParser parser;
  private final Map<Integer, String> nameMap;

  @Inject
  public DefaultCSVIBSchemaInboundRowMapper() {
    this(null, true, new CSVParser(), Collections.emptyMap());
  }

  private DefaultCSVIBSchemaInboundRowMapper(IBSchema in, boolean failOnError, CSVParser p,
      Map<Integer, String> nameMap) {
    super(in, failOnError);
    this.parser = p;
    this.nameMap = Objects.requireNonNull(nameMap);
  }

  @Override
  public String getInboundType() {
    return String.class.getCanonicalName();
  }

  protected char getSeparator() {
    return CSVParser.DEFAULT_SEPARATOR;
  }

  @Override
  public IBSchemaInboundRowMapper<String> configure(IBSchema inboundTargetSchema, ConfigMap cm) {
    boolean foe = cm.getParsedBoolean(FAIL_ON_ERROR, true);
    Map<Integer, String> nMap = cm.get("nameMap");
    char quotechar = cm.getOptionalString("quoteChar").map(s -> s.charAt(0)).orElse(CSVParser.DEFAULT_QUOTE_CHARACTER);
    char escape = cm.getOptionalString("escape").map(s -> s.charAt(0)).orElse(CSVParser.DEFAULT_ESCAPE_CHARACTER);
    boolean ignoreLeadingWhiteSpace = cm.getParsedBoolean("ignoreLeadingWhiteSpace",
        CSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE);
    boolean strictQuotes = cm.getParsedBoolean("strictQuotes", CSVParser.DEFAULT_STRICT_QUOTES);
    CSVParser c = new CSVParser(getSeparator(), quotechar, escape, strictQuotes, ignoreLeadingWhiteSpace);
    return new DefaultCSVIBSchemaInboundRowMapper(inboundTargetSchema, foe, c, nMap);
  }

  @Override
  public Optional<IBDataIntermediary> map(String row) {
    DefaultIBDataIntermediary d = new DefaultIBDataIntermediary(getSchema());
    CSVParser p = ofNullable(this.parser).orElseThrow(() -> new IBDataException("Unconfigured mapper"));
    try {
      String[] vals = p.parseLine(row);
      for (Map.Entry<Integer, String> entry : this.nameMap.entrySet()) {
        Integer i = entry.getKey();
        String n = entry.getValue();
        IBField sf = getSchemaFieldMapByName().get(n);
        if (sf == null) {
          addGenericError("Key Map for name " + n + " not present in schema");
          d = null;
          break;
        } else if (sf.isDeprecated()) {
          addGenericError("Schema field" + n + " is deprecated");
          d = null;
          break;
        } else if (i > vals.length) {
          addGenericError("Key Map for name " + n + " out of range for fields count " + vals.length);
          d = null;
          break;
        } else
          d.put(n, vals[i]);
      }
    } catch (IOException e) {
      addGenericError(e, "Error Mapping " + row);
      d = null;
    }
    return ofNullable(d);
  }

}
