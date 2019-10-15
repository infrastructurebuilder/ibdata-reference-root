# ibdata-reference-root

How to test this:

1. clone this repo
1. ```
rm -rf ~/.m2/repository/org/infrastructurebuilder
mvn clean install
cd integration
cd ibdata-test-ingest
mvn clean install
cd ../ibdata-test-transform
mvn clean install
```
