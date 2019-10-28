# ibdata-avro-types

These tranformers (and finalizers) are for managing Avro types.

## Record Transformers

| Hint | Accepts | Produces | Effect | Parameters |
| ---- | ------- | -------- | ------ | ---------- |
| `map-to-generic-avro` | `Map[String,String]` | `GenericRecord` | Trims lines that are processed String entries (like csv, etc) | * `schema` - Path to schema (avsc)
* `timestamp.formatter` - Timestamp format (Default : `DateTimeFormatter.ISO_ZONED_DATE_TIME`
* `time.formatter` - Time field type formatter (Default: `HH:MM` - 24 hour with hours 00-23 )
* `date.formatter` - Date field type formatter (DEfault: `mm-DD-yy`
* `locale.language`- Locale (Default: default for system)
* `locale.region` - Locale (Default: default for system) |


## Record Finalizers

| Hint | Produces | Effect | Parameters |
| ---- | -------- | ------ | ---------- |
| `avro-generic` |  `avro/binary` | Writes a DataStream of Avro data (MimeType `avro/binary`) | `numberOfRowsToSkip` Skip the supplied number of rows (Default: `0`) |


## Notes
