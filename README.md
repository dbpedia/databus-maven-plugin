# Databus Maven Plugin
Aligning data and software lifecycle with Maven
The plugin provides the following features:
* Maven is a widely used software build automation tool; triggered by the ever-growing importance of data you can now integrate data seamlessly into your software lifecycle with this plugin, i.e. 
  1. import data to be used by the software automatically
  2. compile and run the software as usual, producing new data as output
  3. republish that data on the web and the databus (for other to discover and use with this plugin)
* the plugin can also be used as a *standalone version*, i.e. if you just have a folder with data that you want to publish



<!--run ` ./gh-md-toc --insert README.md` to regenerate -->
# Table of Contents
<!--ts-->
   * [Databus Maven Plugin](#databus-maven-plugin)
   * [Table of Contents](#table-of-contents)
   * [Requirements](#requirements)
      * [Technical requirements](#technical-requirements)
      * [Terminology](#terminology)
      * [Quickstart](#quickstart)
         * [Run an example](#run-an-example)
         * [Create your own (how the example was created)](#create-your-own-how-the-example-was-created)
            * [Step 1: Deploy archetypes into your local repository](#step-1-deploy-archetypes-into-your-local-repository)
            * [Step 2:](#step-2)
   * [License and Contributions](#license-and-contributions)
      * [Development rules](#development-rules)
   * [Phases](#phases)
   * [Usage](#usage)
   * [Documentation of available plugins](#documentation-of-available-plugins)

<!-- Added by: shellmann, at: 2018-08-09T16:47+02:00 -->

<!--te-->
# Requirements
Databus maven plugin philosophy:
*  enforces as few requirements as possible on how you handle your data (wide applicability)
*  automate the data release process as much as possible (medium effort to learn and setup, great time-saver afterwards)


Strict minimal requirements:
* WebID/Private key: in order to guarantee clear provenance
* (under discussion) Same-Origin-Policy: metadata files are required to be published under the same domain as the data, i.e. no third-party rebranding of already published data
* (under discussion) dataid uses relative path and assumes the file lies in the same folder

## Technical requirements
* Maven 3 `sudo apt-get install maven`
* Java 1.7

## Terminology
* Dataset (handled as a maven module): we define a dataset as a collection of files with the same starting prefix ($artifactId). These files normally contain the same or similar kind of data with some variants, e.g. different languages (contenvariant) and formats
* Bundle (handled as parent pom): a collection of datasets (modules) released together, mainly for practical purposes, e.g. they have the same metadata, i.e. publisher, version number, etc. 
Note that the distinction between dataset and bundle is up to the creator, we only require that all files in a dataset start with the same prefix, i.e. the artifactid

## Quickstart
clone the repository
```
git clone https://github.com/dbpedia/databus-maven-plugin.git
cd databus-maven-plugin
```
### Run an example 
There are working examples in the example folder, which you can copy and adapt
`cd example/animals`
validate, parse, generate metadata and package
`mvn databus:validate databus:test-data databus:metadata databus:package-export`

modify output folder
`mvn databus:package-export -Ddatabus.packageDirectory="/var/www/mydata.org/datareleases"` 


### Create your own (how the example was created)
Maven provides a template called archetype. We provide two such templates:
* `bundle-archetype` generates a bundle with one dataset (called add-one-dataset)
* `add-one-dataset-archetype` adds a module to an existing bundle
#### Step 1: Deploy archetypes into your local repository
```
cd archetype/existing-projects
./deploy.sh
```
`deploy.sh` runs mvn install on bundle and bundle/add-one-dataset 

#### Step 2:
configure -DgroupId -DartifactId -Dversion
```
mvn archetype:generate -DarchetypeCatalog=local -DarchetypeArtifactId=bundle-archetype -DarchetypeGroupId=org.dbpedia.databus \
	-DgroupId=org.example -DartifactId="animals" -Dversion="1.0.0" -DinteractiveMode=false`
```

go into the bundle

`cd animals` 

generate a new module "mamals"
```
mvn archetype:generate -DarchetypeCatalog=local -DarchetypeArtifactId=add-one-dataset-archetype -DarchetypeGroupId=org.dbpedia.databus \
	-DgroupId=org.example -DartifactId=mammals -Dversion="1.0.0" -DinteractiveMode=false
```

HOTFIX: adjust mammals/pom.xml and fix the <parent>
(we will try to create an archetype that does it automatically in the future)
```
sed -i "s|<artifactId>bundle</artifactId>|<artifactId>animals</artifactId>|" mammals/pom.xml
sed -i "s|<groupId>org.dbpedia.databus</groupId>|<groupId>org.example</groupId>|" mammals/pom.xml
```

add as many other datasets in the same way
(optional) remove the example dataset folder and the module
```
rm -r add-one-dataset
sed -i  's|<module>add-one-dataset</module>||' pom.xml
```

start editing the pom.xml in animals and the subfolders
remove the example files under src/main/databus/input
copy your data in the modules/subfolders into src/main/databus/input
file names need to start with the artifactId 



# License and Contributions
License of the software is AGPL with intended copyleft. We expect that you spend your best effort to commit upstream to make this tool better or at least that your extensions are made available again. 
Any contribution will be merged under the copyright of the DBpedia Association. 
## Development rules
* All paths are configured in Properties.scala, which is a trait for the Mojos (Maven Plugin classes), please handle all paths there
* Datafile.scala is a quasi decorator for files, use getInputStream to open any file
* Use the issue tracker, do branches instead of forks (we can give access), we will merge with master
* Document options in the archetype pom and here

# Phases
Below we are listing all the phases, that are relevant and describe how the databus-maven-plugin hooks into the maven lifecycle. Not all phases are used, see the [complete reference](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html#Lifecycle_Reference)

Phase | Goal | Description 
--- | --- | ---
validate|`databus:validate`|validate the project is correct and all necessary information is available, especially check the WebId and the private key
generate-resources|not yet implemented|Download the data dependencies
compile| none |compile the source code of the project
  |`exec` | The software has to be executed between compile and test in order to produce the data
test|`databus:test-data` | Parses all data files to check for correctness, generates a parselog for inclusion in the package. `-DskipTests=true` skips this phase, as it requires some time to run
prepare-package|`databus:metadata`|Analyses each file and prepares the metadata
prepare-package|`databus:rss`|TODO Not implemented yet
package| |take the compiled code and package it in its distributable format
verify| |run any checks on results of integration tests to ensure quality criteria are met
install| |install the package into the local repository, for use as a dependency in other projects locally
deploy| |done in the build environment, copies the final package to the remote repository for sharing with other developers and projects.

# Usage

The configuration is documented in the example pom.xml: https://github.com/dbpedia/databus-maven-plugin/blob/master/example/animals/pom.xml

Once you have downloaded the pom.xml from this project and configured it properly, you can use the maven commands as specified in the phases, e.g. `mvn databus:validate`, `mvn databus:test-data`, `mvn databust:metadata`, `mvn databus:package-export`


# Documentation of available plugins
user contributed plugins
<!--
# Problem
Publishing data on the web in a de-centralised manner is the grand vision of the Semantic Web. However, decentralisation comes with its problems. Putting data files on your web server and creating a landing page to describe this data, just goes a short way. Humans can read the landing page and use the right-click save-as to download the files. Crawlers can discover links and can download the files automatically, but have no understanding of the context, publisher, version or other metadata of the files, making its usage limited. 

# Solution
With the databus-maven-plugin you are able to manage and release your data like software. The databus-maven-plugin will help you in producing good metadata and will analyse youur data files for errors as well as generate statistics and finally sign the files with your private key, so all downloaders can verify its integrity. 
Once you publish your data on your own web server, you can ping the databus to collect the metadata and index your data release. This has many advantages for you as a data publisher:
1. your data can be found more easily and trusted due to your signature
2. the databus will push your metadata ownwards to other metadata repositories, so you are even better indexed
3. Databus tools and services from the community will run on your data providing demos and enrichement, so you can benefit from the power of the DBpedia dev community
4. You can browse and setup additional features on the databus
 
# Storage and Tools
 Note that the databus as well as this tool is still in early beta, in order to assess any needs and wishes, we prepared this form to apply as a beta tester:
 TODO 

# How to use this repo

## How to publish data

The repo provides two assets:
1. the Databus Maven Plugin to build your data releases with Maven
2. A template maven project that you can download and adjust
Furthermore, the repo also contains the configuration of the DBpedia Core Releases for you to look at and adapt.  

### Setup
1. Dowload the maven project template (
TODO, at the moment we only have the DBpedia ones

2. Adjust the information in the pom.xml
explain ArtifactID, content, format and compressionvariant

3. add your webid and private key
```
TODO where
```

4. copy your data files under `src/main/resources`


### Lifecycles of the plugin
The instruction here follow the maven lifecycle for releasing software (https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html#Lifecycle_Reference), the databus-maven-plugin implements the parts that are necessary to adjust the software lifecycle to the data lifecycle.
In particular the follwoing phases are adjusted.

#### Validate
The plugin checks whether you entered all information correctly, cf. https://github.com/dbpedia/data-release-tool-ti/blob/master/databusmavenplugin/src/main/scala/org/dbepdia/databus/Validate.scala
Version number x.y.z, etc. 
`mvn databus:validate`

```
<execution>
	<id>validate</id>
	<phase>validate</phase>
	<goals>
		<goal>validate</goal>
	</goals>
</execution>
```


#### Generate Resources, Goal analysis
`mvn databus:analysis`

```
<execution>
	<id>analysis</id>
	<phase>process-resources</phase>
	<goals>
		<goal>analysis</goal>
	</goals>
</execution>
```

The plugin will analyse each file that you put under src/main/resourcs to:
1. assess compression used
2. assess format of data
3. assess size
4. parse and validate RDF
5. generate statistics for the links
6. create md5checksum
7. create signature 
cf. https://github.com/dbpedia/data-release-tool-ti/blob/master/databusmavenplugin/src/main/scala/org/dbepdia/databus/FileAnalysis.scala

For each file the plugin will create a local dataid file, i.e. if your file is xxx.ttl.bz2 the FileAnalysis will create xxx.ttl.bz2.data.ttl

#### prepare-package
generate one dataid file for each artifactid out of the local dataid files

#### Deploy, Goal deploy-local
TODO: decide whether this is one goal with different configs or several goals and only one is called, eg. `mvn databus:ckan`
`mvn databus:deploy-local`
This step needs to be adapted by you the most as it depends on where you will host the files, here are some options:

* if you host the data files on the same server as you run maven, you can copy the resources to `/var/www`
* if you run remote, you can upload via ssh or sftp
* many other methods exist

#### Deploy, Goal databus-deploy
`mvn databus:databus-deploy`

Announces the uris of the main dataid for each artifact id to the databus for indexing.  

# misc
```
# command to convert pem to der format 
openssl pkcs8 -topk8 -inform PEM -outform DER -in private_key_dev_dummy.pem -out private_key_dev_dummy.der -nocrypt
```


# Add-ons and formats 
For future reference

## List of versioning approaches

## List of notifications 
* https://www.openarchives.org/rs/toc
* https://www.openarchives.org/rs/notification/1.0.1/notification
* http://aksw.org/Projects/SemanticPingback.html
* https://en.wikipedia.org/wiki/WebSub
* (not working) https://www.programmableweb.com/api/ping-semantic-web

## List of repos for upload of metadata
* https://www.sciencebase.gov/catalog/ 
* DataHub ?
* LingHub ?
* LOD Cloud ? 



## Requirements for development
```
sudo apt-get install scala
```

```
mvn install 
```

## Usage

```
<repositories>
    <repository>
        <id>data-release-tool-ti-mvn-repo</id>
        <url>https://raw.github.com/dbpedia/data-release-tool-ti/mvn-repo/</url>
        <snapshots>
            <enabled>true</enabled>
            <updatePolicy>always</updatePolicy>
        </snapshots>
    </repository>
</repositories>

```
-->



