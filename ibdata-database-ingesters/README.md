# ibdata-database-ingesters

This module provides ingesters and finalizers for ingesting data directly from a database.

Note that the use of this module effectively terminates provenance of data at the time of ingestion.  It is unlikely
that a repeat execution of a database ingestion will produce exactly the same results unless extreme measures are taken to
ensure that this happens.  This includes providing proper sorting as well as ensuring that no actual changes were
made to the data.

The Infrastructure Builder team recommends that you never use this ingester.  It is, and will remain supported.  However, database
queries violate the ingestion construct with alarming frequency.  Using this ingester is almost always an indicator that you
are trying to take a dangerous shortcut.

You have been warned.


## Ingesters

| Hint | Effect | Parameters |
| --- | ------ | ---------- |
| `jdbc` | Selects records from a query and allows the finalizer to write them as Avro | <ul><li>`dialect` [org.jooq.SQLDialect](https://www.jooq.org/javadoc/3.12.x/org.jooq/org/jooq/SQLDialect.html) [Jooq dialect](https://www.jooq.org/doc/3.12/manual/sql-building/dsl-context/sql-dialects/) </li><li>`query` SQL Query to execute.  Not validated.</li> <li>`schema` Avro schema to write records as.  Optional.  If not provided, the system will attempt to infer.  If provided, must contain all fields in the `query` result</li><li>`namespace` Namespace if not schema is provided.  If not provided, the namespace is set to `org.infrastructurebuilder.data`</ul>|



## Finalizers

| Hint | Accepts | Produces | Effect | Parameters |
| ---- | ------- | -------- | ------ | ---------- |
| `default-jdbc` | Database input | `binary/avro` | Writes an Avro stream of records mapped from a query | `numberOfRowsToSkip` Skip the supplied number of rows (Default: `0`)  |


## Database Ingestion Process

Ingesting databases is slightly different from file or URL-based resources.

## Tables Only

Database ingestion only works at the table level.  IBData considers a datastream as if it were a single table, with a single unencumbered schema, so
it makes sense to discuss ingestion and export at the table level.

It might be possible to produce a temporary table prior to ingestion, but the result is the same:  one "table", one datastream.

## The Ingestion

### Identity

The name of an ingestion is always the name of the target table in the database.

### The Actual Process

A database ingestion always consists of two things:

1. Obtaining the schema, either an existing one or an IBData schema extraction from the query
1. A data extraction of the query data.

In the first case, a minimum of two datastreams will be created for a database ingestion.  The first will be the schema, the creation of which might be a
repeatable event.  The second will be the data itself.  This stream, per above, will likely never be identical unless explicit sorting of a
query of an unchanging dataset is repeated.

