# Databus Maven Plugin
Aligning data and software lifecycle with Maven

The plugin was developed to use the features of the Maven software build automation tool for data releases and metadata generation.
The tool has the following features:
* once configured properly (1-3 hours), data can be released and released systematically in minutes
* auto-detect RDF formats with syntax validation 
* RDF is NOT a requirement, any data can be released (binary, csv, xml), however with RDF the tool has more features
* auto-detect compression variant
* private key signature, sha256sum and provenance (WebID) generation
* generation of metadata compatible to:
  * RSS feeds

## Metadata Standards
DBpedia's DataID fulfills 31 of 35 Best Practices from the W3C Data on the Web Best Practices Working Group, cf. [implementation report](http://w3c.github.io/dwbp/dwbp-implementation-report.html) 

<img title="DWBP Implementation Report Summary" width="400" src="https://raw.githubusercontent.com/dbpedia/databus-maven-plugin/master/DWBP.png" ></img>


## Roadmap
We are planning the following features:
* DCAT and DCAT-AP interoperability
* FAIR Data principles
* automatic generation of H2020 EU Data Management Plan Deliverables 
  * feature exists, but is not yet integrated:
  * https://wiki.dbpedia.org/use-cases/data-management-plan-extension-dataid
* automatic upload of metadata to other repositories:
  * http://lod-cloud.net
  * CKAN
  * RE3
 
 Did we forget something? suggest more interoperability in the issue tracker: https://github.com/dbpedia/databus-maven-plugin/issues





<!--run ` ./gh-md-toc --insert README.md` to regenerate -->
# Table of Contents
<!--ts-->
   * [Databus Maven Plugin](#databus-maven-plugin)
      * [Roadmap](#roadmap)
   * [Table of Contents](#table-of-contents)
   * [Bundle, dataset, distribution](#bundle-dataset-distribution)
      * [Terminology](#terminology)
      * [Relation to Maven](#relation-to-maven)
      * [Versioning](#versioning)
      * [Files &amp; folders](#files--folders)
         * [(Important) File input path](#important-file-input-path)
         * [(Important) File copying](#important-file-copying)
   * [Usage](#usage)
      * [How to make a release](#how-to-make-a-release)
         * [Github setup](#github-setup)
      * [Change version of the whole bundle](#change-version-of-the-whole-bundle)
   * [Run the example](#run-the-example)
   * [Configuration](#configuration)
      * [File setup and conventions](#file-setup-and-conventions)
      * [Generate a release configuration with an archetype](#generate-a-release-configuration-with-an-archetype)
         * [Install databus archetype](#install-databus-archetype)
         * [Instantiate a new project](#instantiate-a-new-project)
   * [Development](#development)
      * [License](#license)
      * [Development rules](#development-rules)
   * [Troubleshooting](#troubleshooting)
      * [BUILD FAILURE, no mojo-descriptors found (when using mvn install to install the databus-maven-plugin)](#build-failure-no-mojo-descriptors-found-when-using-mvn-install-to-install-the-databus-maven-plugin)

<!-- Added by: shellmann, at: 2018-09-07T15:42+02:00 -->

<!--te-->

# Bundle, dataset, distribution
In this section, we will describe the basic terminology and how they relate to Maven. 

## Terminology
* Dataset - a dataset is a bunch of files that have a common description. The fact that they can be described together shows an inherent coherence and that they belong together. Other than this criteria, it is quite arbitrary how datasets are defined, so this is a pragmatical approach, i.e. there is no need to duplicate documentation, i.e. have several datasets with the same description or subspecialisation, i.e. this dataset is about X, but some files are about Y
   * the databus maven plugin *requires that all files of a dataset start with the datasetname* 
* Distribution - one file of a dataset
* Formatvariance - a dataset can have files in different formats. Format variance is abstracted quite well, different distributions are created with same metadata except for the format field
* Compression variance - compression is handled separatedly from format, i.e. the same format can be compressed in different ways
* Contentvariance of a dataset - besides the format variance a dataset can have a certain degree of content variance. This normally determines how the dataset is distributed over the files. The easiest example is DBpedia, where each dataset contains all Wikipedia languages, so in this case contentvariance is the language. The data could also be differentiated by type, e.g. a company dataset that produces a distribution for each organsiation form (non-profit, company, etc). As a guideline, contentvariance can be choosen arbitrarily and the only criteria is whether there are some use cases, where users would only want part of the dataset, otherwise merging into one big file is fine. 
* Bundle - a collection of datasets released together. Also a pragmatic definition. The framework here will not work well, if you combine datasets with different release cycles and metadata in the same bundle, e.g. some daily, some monthly or metadata variance different publishers or versioning systems.

## Relation to Maven
Maven was established to automate software builds and release them (mostly Java). A major outcome of the ALIGNED project (http://aligned-project.eu/) was to establish which parts of data releases can be captured by Maven. Here is a practical summary:

Maven uses a Parent POM (Project Object Model) to define software project. The POM is saved in a file called `pom.xml`. Each project can have multiple `modules` where the code resides. These modules refer to the parent pom and inherit any values unless they are overwritten. While in software the programming language defines a complex structure which has to be followed, in data everything is fantasy ecxept for the concrete file as it provides a clearly defined thing. Hence the model imposed for the databus is simpler than for software:
* Bundle relates to the Parent POM and inherits its metadata to the modules/datasets
* Datasets are modules and receive their metadata from the bundle/parent pom (and can extend or override it)
* Distributions are the files of the dataset and are normally stored in `src/main/databus/${version}/` for each module
* Each dataset/module has its own artifactid, the distributions/files must start with the artifactid


## Versioning
Changes in software can be tracked very well and manual versioning can be given. Data behaves two-fold: Schematic information, e.g. schema definitions, taxonomy and ontologies can be versioned like software. The data itself follows pareto-efficiency: The first 80% need 20% of effort, the last 20% need 80%. Fixing the last error in data is extremely expensive. Hence, we recommend using a time-based version, i.e. YEAR.MONTH.DAY in the format YYYY.MM.DD (alphabetical sortable). Another possibility is to align the version number to either:
1. the software version used to create it (as a consequence the software version needs to be incremented for each data release)
2. the ontology version if and only if the ontology is contained in the bundle and versioned like software

## Files & folders
Per default 
```
${bundle}/ 
+-- pom.xml (parent pom with bundle metadata and ${version}, all artifactids are listed as `<modules>` )
+-- ${artifactid1}/ (module with artifactid as datasetname)
|   +-- pom.xml (dataset metadata)
|   +-- src/main/databus/${version}/
|   |   *-- ${artifactid1}_cvar1.nt (distribution, content variance 1, formatvariance nt, compressionvariant none)
|   |   *-- ${artifactid1}_cvar1.csv (distribution, content variance 1, formatvariance csv, compressionvariant none)
|   |   *-- ${artifactid1}_cvar1.csv.bz2 (distribution, content variance 1, formatvariance csv, compressionvariant bzip)
|   |   *-- ${artifactid1}_cvar2.ttl (distribution, content variance 2, formatvariance ttl, compressionvariant none)
|   |   *-- ${artifactid1}_cvar2.csv (distribution, content variance 2, formatvariance csv, compressionvariant none)
```
An example is given in the example folder of this repo.

### (Important) File input path
The file input path is `src/main/databus/${version}/` per default, relative to the module.
This path can be configured in the parent pom.xml using the `<databus.dataInputDirectory>` parameter. Absolute paths are allowed. 

### (Important) File copying
During the maven build process, the code is normally duplicated 6-7 times. For each module, the code is first copied and compiled in the `target/classes` folder and then copied and compressed again in a .jar file. All this is then copied again. 
The databus-maven-plugin behaves different: 
* the `target/databus` folder is used to assemble metadata (which is not large)
* `mvn clean` deletes the target folder and will only delete the generated metadata
* no input data is copied into the `target` folder, i.e. the process does not duplicate data due to storage reasons
* `mvn databus:package-export` will copy the files to an external location as given in `<databus.packageDirectory>`.
 

# Usage 

## How to make a release 
Once the project is configured properly [see Configuration](#configuration) releases are easy to generate and update. 
The only technical requirement for usage is Maven3 `sudo apt-get install maven`
We regularly deploy the plugin to our archiva at http://databus.dbpedia.org:8081/, later Maven Central.
Maven will automatically install the plugin (Note that the archetype for configuration has to be installed manually at the moment )
We assume that you have set up the private key, the WebId and the data resides in `src/main/databus/${version}/` and the pom.xml are configured properly.

```
# deleting any previously generated metadata
mvn clean 

# validate setup of private key/webid
mvn databus:validate

# validate syntax of rdf data, generated parselogs in target/databus/parselogs
# Note: this is a resource intensive step. It can be skipped (-DskipTests=true)
mvn databus:test-data

# generate metadata in target/databus/dataid
mvn databus:metadata

# export the release to a local directory as given in <databus.packageDirectory>
# copies data from src, metadata and parselogs from data
mvn databus:package-export

# output folder or any parameter can be set on the fly 
mvn databus:package-export -Ddatabus.packageDirectory="/var/www/mydata.org/datareleases"


```


### Github setup
The pom.xml can be versioned via GitHub as we do for `dbpedia` (see folder). Add the following to `.gitignore` to exclude data from being committed to git:
`${bundlefolder}/*/*/src/`

## Change version of the whole bundle
`mvn versions:set -DnewVersion=2018.08.15`


# Run the example
There are working examples in the example folder, which you can copy and adapt

```
# clone the repository
git clone https://github.com/dbpedia/databus-maven-plugin.git
cd databus-maven-plugin
cd example/animals

# validate, parse, generate metadata and package
mvn databus:validate databus:test-data databus:metadata databus:package-export

```



# Configuration

## File setup and conventions

## Generate a release configuration with an archetype
 Note: For datasets with few artifacts, you can also copy the example and adjust it

We provide a Maven Archetype for easy and automatic project setup. In short, Archetype is a Maven project templating toolkit: https://maven.apache.org/guides/introduction/introduction-to-archetypes.html 
The template is created from an existing project, found in `archetype/existing-projects`. Variables are replaced upon instantiation. 

### Install databus archetype 
We provide two archetype templates:
* `bundle-archetype` generates a bundle with one dataset (called add-one-dataset)
* `add-one-dataset-archetype` adds a module to an existing bundle

The archetype needs to be installed into the local maven repo:
```
git clone https://github.com/dbpedia/databus-maven-plugin.git
cd databus-maven-plugin/archetype/existing-projects
./deploy.sh
```
`deploy.sh` runs `mvn archetype:create-from-project` and `mvn install` on bundle and bundle/add-one-dataset 

### Instantiate a new project
With the archetype you can create one bundle with arbitrarily many datasets/artifacts. Here is how:

```
# Generate the bundle

# version number of bundle
VERSION=2018.08.15
# domain 
GROUPID=org.example.data
# bundle artifactid
BUNDLEARTIFACTID=animals
# configure list of datasets/artifacts to be created
DATASETARTIFACTID="mammals birds fish"

mvn archetype:generate -DarchetypeCatalog=local -DarchetypeArtifactId=bundle-archetype -DarchetypeGroupId=org.dbpedia.databus.archetype -DgroupId=$GROUPID -DartifactId=$BUNDLEARTIFACTID -Dversion=$VERSION -DinteractiveMode=false

# Generate datasets/modules 

# go into the bundle
cd $BUNDLEARTIFACTID

for i in ${DATASETARTIFACTID} ; do 
	mvn archetype:generate -DarchetypeCatalog=local -DarchetypeArtifactId=add-one-dataset-archetype -DarchetypeGroupId=org.dbpedia.databus.archetype -DgroupId=$GROUPID -DartifactId=$i -Dversion=$VERSION -DinteractiveMode=false
	# some clean up, since archetype does not set parent automatically  
	# TODO we are trying to figure out how to automate this
	sed -i "s|<artifactId>bundle</artifactId>|<artifactId>$BUNDLEARTIFACTID</artifactId>|" */pom.xml
	sed -i "s|<groupId>org.dbpedia.databus.archetype</groupId>|<groupId>$GROUPID</groupId>|" */pom.xml
	sed -i "s|<version>1.0.0</version>|<version>$VERSION</version>|" */pom.xml
done



# delete add-one-dataset 
rm -r add-one-dataset
sed -i  's|<module>add-one-dataset</module>||' pom.xml

# wipe the example data files
rm */src/main/databus/$VERSION/*
```

# Development 

## License
License of the software is AGPL with intended copyleft. We expect that you spend your best effort to commit upstream to make this tool better or at least that your extensions are made available again. 
Any contribution will be merged under the copyright of the DBpedia Association. 
## Development rules
* All paths are configured in Properties.scala, which is a trait for the Mojos (Maven Plugin classes), please handle all paths there
* Datafile.scala is a quasi decorator for files, use getInputStream to open any file
* Use the issue tracker, do branches instead of forks (we can give access), we will merge with master
* Document options in the archetype pom and here


<!--



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

Once you have downloaded the pom.xml from this project and configured it properly, you can use the maven commands as specified in the phases, e.g. `mvn databus:validate`, `mvn databus:test-data`, `mvn databus:metadata`, `mvn databus:package-export`


# Documentation of available plugins
user contributed plugins



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


#### prepare-package, Goal metadata
`mvn databus:metadata`

```
<execution>
	<id>metadata</id>
	<phase>prepare-package</phase>
	<goals>
		<goal>metadata</goal>
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
# Troubleshooting
## Download from http://databus.dbpedia.org:8081/repository/ fails, no dependency information available
  Note: this section can be removed after completion of https://github.com/dbpedia/databus-maven-plugin/issues/12 
Possible reason: we have installed a dev archiva for now. Depending on your org's network configuration, code might only be accepted from Maven Central and local/allowed maven repos.
* `[WARNING] The POM for org.dbpedia.databus:databus-maven-plugin:jar:1.0-SNAPSHOT is missing, no dependency information available`
* `Could not resolve dependencies for project org.dbpedia.databus:databus-maven-plugin:maven-plugin:1.0-SNAPSHOT: Failure to find org.dbpedia.databus:databus-shared-lib:jar:0.1.4`
Can potentially fixed by locally installing the shared-lib:
* Download Jar: http://databus.dbpedia.org:8081/#artifact-details-download-content/org.dbpedia.databus/databus-shared-lib/0.1.4
* Install dependency: https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html

Then clone the repo and run `mvn install` which will install the databus-maven-plugin locally



## BUILD FAILURE, no mojo-descriptors found (when using `mvn install` to install the databus-maven-plugin)
This is most likely caused by using an old maven version (observed in version `3.0.5`)
A workaround for this would be replacing:
```
<plugin>
	<groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-plugin-plugin</artifactId>
	<version>3.4</version>
</plugin>
```
with
```
<plugin>
	<groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-plugin-plugin</artifactId>
	<version>3.4</version>
        <configuration>
		<skipErrorNoDescriptorsFound>true</skipErrorNoDescriptorsFound>
	</configuration>
        <executions>
		<execution>
		        <id>mojo-descriptor</id>
		        <goals>
                            <goal>descriptor</goal>
                        </goals>
		</execution>
	</executions>
</plugin>

``` 
in `databus-maven-plugin/pom.xml`


