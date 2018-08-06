#/bin/bash
# change version in pom and here
VERSION=1.0.0

cd bundle
mvn clean
TMP=$PWD

# generate archetype for the modules 
cd add-one-dataset
mvn archetype:create-from-project
cd target/generated-sources/archetype/
mvn deploy:deploy-file -Dfile=target/add-one-dataset-archetype-$VERSION.jar -DpomFile=pom.xml -DrepositoryId="archiva.internal" -Durl="http://databus.dbpedia.org:8081/repository/internal"


# 
cd $TMP
mvn archetype:create-from-project
cd target/generated-sources/archetype/
mvn deploy:deploy-file -Dfile=target/bundle-archetype-$VERSION.jar -DpomFile=pom.xml -DrepositoryId="archiva.internal" -Durl="http://databus.dbpedia.org:8081/repository/internal"

