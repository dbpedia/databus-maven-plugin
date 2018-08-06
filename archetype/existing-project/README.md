
```
cd bundle
mvn clean
TMP=$PWD

cd dataset
mvn archetype:create-from-project
cd target/generated-sources/archetype/
mvn install 

cd $TMP
mvn archetype:create-from-project
cd target/generated-sources/archetype/
mvn install 
