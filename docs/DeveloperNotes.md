# Developer notes
* configuration values taken from Maven are configured in `Properties.scala`, use its 'sub-trait' `Locations.scala` to derive filesystem locations from these and `Parameters.scala` to compute all other values derived from the original Maven properties (refactoring into this separation is not yet complete, but please heed this guidelines for additional   configuration-derived fields nontheless)
* Datafile.scala is a quasi decorator for files, use getInputStream to open any file
* Use the issue tracker, do branches instead of forks (we can give access), we will merge with master

## Maven Archiva 

We are running a Maven Archiva for Snapshots. 
New snapshots can be deployed with `mvn deploy` as configured in the pom.xml. Password is needed to be entered in `~/.m2/settings.xml`

<!--

# Bundle, dataset, distribution
In this section, we will describe the basic terminology and how they relate to Maven. 

## Terminology
* Dataset - a dataset is a bunch of files that have a common description. The fact that they can be described together shows an inherent coherence and that they belong together. Other than this criteria, it is quite arbitrary how datasets are defined, so this is a pragmatical approach, i.e. there is no need to duplicate documentation, i.e. have several datasets with the same description or subspecialisation, i.e. this dataset is about X, but some files are about Y
   * the databus maven plugin *requires that all files of a dataset start with the datasetname* 
* Distribution - one file of a dataset
* Formatvariance - a dataset can have files in different formats. Format variance is abstracted quite well, different distributions are created with same metadata except for the format field
* Compression variance - compression is handled separatedly from format, i.e. the same format can be compressed in different ways
* Contentvariance of a dataset - besides the format variance a dataset can have a certain degree of content variance. This normally determines how the dataset is distributed over the files. The easiest example is DBpedia, where each dataset contains all Wikipedia languages, so in this case contentvariance is the language. The data could also be differentiated by type, e.g. a company dataset that produces a distribution for each organsiation form (non-profit, company, etc). As a guideline, contentvariance can be choosen arbitrarily and the only criteria is whether there are some use cases, where users would only want part of the dataset, otherwise merging into one big file is fine. 
* Group - a collection of datasets released together. Also a pragmatic definition. The framework here will not work well, if you combine datasets with different release cycles and metadata in the same bundle, e.g. some daily, some monthly or metadata variance different publishers or versioning systems.

## Relation to Maven
Maven was established to automate software builds and release them (mostly Java). A major outcome of the ALIGNED project 
(http://aligned-project.eu/) was to establish which parts of data releases can be captured by Maven. Here is a 
practical summary:

Maven uses a Parent POM (Project Object Model) to define software project. The POM is saved in a file called `pom.xml`. 
Each project can have multiple `modules` where the code resides. These modules refer to the parent pom and inherit any 
values unless they are overwritten. While in software the programming language defines a complex structure which has to 
be followed, in data everything is fantasy ecxept for the concrete file as it provides a clearly defined thing. Hence 
the model imposed for the databus is simpler than for software:
* Bundle relates to the Parent POM and inherits its metadata to the modules/datasets
* Datasets are modules and receive their metadata from the bundle/parent pom (and can extend or override it)
* Distributions are the files of the dataset and are normally stored in `src/main/databus/${version}/` for each module
* Each dataset/module has its own artifactid, the distributions/files must start with the artifactid


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
