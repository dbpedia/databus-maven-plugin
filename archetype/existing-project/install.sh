#!/bin/bash

# change version in pom and here
VERSION=1.0.0

pushd bundle
mvn clean

# generate archetype for artifacts in bundles
pushd dataset
mvn -Darchetype.properties=../../dataset-archetype.properties archetype:create-from-project
cd target/generated-sources/archetype/
mvn install
popd

# generate archetype for bundles
mvn -Darchetype.properties=../bundle-archetype.properties archetype:create-from-project
cd target/generated-sources/archetype/
mvn install
