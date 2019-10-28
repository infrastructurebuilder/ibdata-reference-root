# ibdata-default-transformers

Just a quick list of the "default" transformers.  These are available from this artifact (`ibdata-default-transformers`).

## Transformers


| Hint | Effect | Parameters |
| ---- | ------ | ---------- |
| `pass-thru` | Does nothing | none |
| `add-stream` | Brute-force add a stream to the final DataSet | `addedPath` Path to the new stream (moving to a URL) |


## Record Transformers

| Hint | Accepts | Produces | Effect | Parameters |
| ---- | ------- | -------- | ------ | ---------- |
| `string-trim` | `String` | `String` | Trims lines that are processed String entries (like csv, etc) | none |
| `regex-line-filter` | `String` | `String` | Filters lines based on a supplied regex.  | `regex` - filtering regex (Defaut: `.*`) |
| `random-line-filter` | Anything | Same as Accepted Type | Filters lines based on a random number generator.  | `percentage` - Percentage of lines as a floating point number (Default : `.5`) |
| `regex-array-split` | `String` | `Array[String]` | Splits a line (like a CSV) to an array using a simple regex  | `regex` - Value to split on (Default : `,`) |
| `array-to-numbered-column` | `Array[String]` | `Map[String,String]` | Maps an array to a map with keys based on a pattern supplied with the index  | `format` - Map key format (Default : `COLUMN%00d`) |
| `array-to-name-map` | `Array[String]` | `Map[String,String]` | Maps an array to a map with keys based on a list of keys index by field position within the array | `fields` - a List of fields ( [see below](#list-of-fields) ) (Default : none) |


## List of Fields

The list of fields in the `array-to-name-map` is described in the `<configuration>` element as follows:

```
  <configuration>
    <fields>
      <field>field_name_1</field>
      <field>field_name_2</field>
      ...
      <field>last_Field_named</field>
    </fields>
  </configuration>
```

