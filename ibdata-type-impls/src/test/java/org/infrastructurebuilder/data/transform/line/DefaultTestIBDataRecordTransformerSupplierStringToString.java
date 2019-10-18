package org.infrastructurebuilder.data.transform.line;

import java.nio.file.Path;
import java.util.Map;

import org.infrastructurebuilder.util.config.ConfigMapSupplier;
import org.infrastructurebuilder.util.config.PathSupplier;

public class DefaultTestIBDataRecordTransformerSupplierStringToString
    extends AbstractIBDataRecordTransformerSupplier<String, String> {

  protected DefaultTestIBDataRecordTransformerSupplierStringToString(PathSupplier wps, ConfigMapSupplier cms) {
    super(wps, cms);
  }

  @Override
  public String getHint() {
    return DefaultTestIBDataRecordTransformerSupplierStringToString.class.getCanonicalName();
  }

  @Override
  public AbstractIBDataRecordTransformerSupplier<String, String> configure(ConfigMapSupplier cms) {
    return this;
  }

  @Override
  protected IBDataRecordTransformer<String, String> getUnconfiguredTransformerInstance(Path workingPath) {
    return new StringToStringRecordTransformer(getWps().get());
  }

  public static class StringToStringRecordTransformer extends AbstractIBDataRecordTransformer<String, String> {

    public StringToStringRecordTransformer(Path path) {
      super(path);
    }

    private StringToStringRecordTransformer(Path workingPath, Map<String, String> config) {
      super(workingPath, config);
    }

    @Override
    public String getHint() {
      return StringToStringRecordTransformer.class.getCanonicalName();
    }

    @Override
    public IBDataRecordTransformer<String,String> configure(Map<String,String> config) {
      return new StringToStringRecordTransformer(getWorkingPath(), config);
    }

    @Override
    public String apply(String t) {
      return t;
    }

  }

}
