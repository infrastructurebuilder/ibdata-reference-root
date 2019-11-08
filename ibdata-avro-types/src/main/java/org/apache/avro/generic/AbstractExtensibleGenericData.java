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
package org.apache.avro.generic;

/**
 * This class is a direct extension of GenericData with differences:
 * 1. It's abstract
 * 2. It provides unprotected accessors to the conversions maps.  This lets another
 *      class extend it further.
 *
 * Note that for whatever reason, avro makes interal calls to GenericData.get() <b>a lot</b>
 * and those calls won't use this class
 *
 * @author mykel.alvis
 *
 */
//abstract public class AbstractExtensibleGenericData extends GenericData {
//
//  private Map<String, PreConversion<?, ?>> _conversions = new HashMap<>();
//
//  private Map<Class<?>, Map<String, PreConversion<?, ?>>> _conversionsByClass = new IdentityHashMap<>();
//
//  public Collection<PreConversion<?, ?>> getPreConversions() {
//    return _conversions.values();
//  }
//
//  public Map<String, PreConversion<?, ?>> getPreConversionsMap() {
//    return _conversions;
//  }
//
//  public Map<Class<?>, Map<String, PreConversion<?, ?>>> getPreConversionsByClass() {
//    return _conversionsByClass;
//  }
//
//  public void addLogicalTypePreConversion(PreConversion<?, ?> conversion) {
//    getPreConversionsMap().put(conversion.getLogicalTypeName(), conversion);
//    Class<?> type = conversion.getPreconversionType();
//    if (!getPreConversionsByClass().containsKey(type)) {
//      getPreConversionsByClass().put(type, new LinkedHashMap<>());
//    }
//    getPreConversionsByClass().get(type).put(conversion.getLogicalTypeName(), conversion);
//  }
//
//  /**
//   * Returns the Conversion for the given logical type.
//   *
//   * @param logicalType a logical type
//   * @return the conversion for the logical type, or null
//   */
//  @SuppressWarnings("unchecked")
//  public Conversion<Object> getConversionFor(LogicalType logicalType) {
//    if (logicalType == null) {
//      return null;
//    }
//    Conversion<Object> k = (Conversion<Object>) getPreConversionsMap().get(logicalType.getName());
//    return k == null ? super.getConversionFor(logicalType) : k;
//  }
//
//  /**
//   * Returns the first conversion found for the given class.
//   *
//   * @param datumClass a Class
//   * @return the first registered conversion for the class, or null
//   */
//  @SuppressWarnings("unchecked")
//  public <T> Conversion<T> getConversionByClass(Class<T> datumClass) {
//    Map<String, PreConversion<?, ?>> conversions = getPreConversionsByClass().get(datumClass);
//    if (conversions != null) {
//      return (Conversion<T>) conversions.values().iterator().next();
//    }
//    return super.getConversionByClass(datumClass);
//  }
//
//  /**
//   * Returns the conversion for the given class and logical type.
//   *
//   * @param datumClass  a Class
//   * @param logicalType a LogicalType
//   * @return the conversion for the class and logical type, or null
//   */
//  @SuppressWarnings("unchecked")
//  public <T> Conversion<T> getConversionByClass(Class<T> datumClass, LogicalType logicalType) {
//    Map<String, PreConversion<?, ?>> conversions = getPreConversionsByClass().get(datumClass);
//    if (conversions != null) {
//      return (Conversion<T>) conversions.get(logicalType.getName());
//    }
//    return super.getConversionByClass(datumClass, logicalType);
//  }
//
//  /**
//   * Return the index for a datum within a union. Implemented with
//   * {@link Schema#getIndexNamed(String)} and {@link #getSchemaName(Object)}.
//   */
//  public int resolveUnion(Schema union, Object datum) {
//    // if there is a logical type that works, use it first
//    // this allows logical type concrete classes to overlap with supported ones
//    // for example, a conversion could return a map
//    if (datum != null) {
//      Map<String, PreConversion<?, ?>> conversions = getPreConversionsByClass().get(datum.getClass());
//      if (conversions != null) {
//        List<Schema> candidates = union.getTypes();
//        for (int i = 0; i < candidates.size(); i += 1) {
//          LogicalType candidateType = candidates.get(i).getLogicalType();
//          if (candidateType != null) {
//            Conversion<?> conversion = conversions.get(candidateType.getName());
//            if (conversion != null) {
//              return i;
//            }
//          }
//        }
//      } else
//        return super.resolveUnion(union, datum);
//    }
//
//    Integer i = union.getIndexNamed(getSchemaName(datum));
//    if (i != null)
//      return i;
//    throw new UnresolvedUnionException(union, datum);
//  }
//
//}
