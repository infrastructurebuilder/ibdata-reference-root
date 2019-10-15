# ibdata-reference-root

How to test this:

First, clone this repo (obvs)
Then:

```
rm -rf ~/.m2/repository/org/infrastructurebuilder
mvn clean install
cd integration
cd ibdata-test-ingest
mvn clean install
cd ../ibdata-test-transform
mvn clean install
```

At it's end, each Maven build should show:

```
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```