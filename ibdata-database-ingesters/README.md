# ibdata-database-ingesters

This module provides ingesters and finalizers for ingesting data directly from a database.

Note that the use of this module effectively terminates provenance of data at the time of ingestion.  It is unlikely
that a repeat execution of a database ingestion will produce exactly the same results unless measures are taken to
ensure that this happens.  This includes providing proper sorting as well as ensuring that no actual changes were
made to the data.

You have been warned.


## Ingesters


| Hint | Effect | Parameters |
| ---- | ------ | ---------- |
| `jdbc` | Does nothing | none |



## Finalizers

| Hint | Accepts | Produces | Effect | Parameters |
| ---- | ------- | -------- | ------ | ---------- |
| `default-jdbc` | Database input | `binary/avro` | Writes an Avro stream of records mapped from a query | <ul><li>`numberOfRowsToSkip` Skip the supplied number of rows (Default: `0`) </li> <li>`query` : select statement to execute (not pre-validated)</li> <ul> |
