# Databus Maven Plugin 
[![Build Status](https://travis-ci.org/dbpedia/databus-maven-plugin.svg?branch=master)](https://travis-ci.org/dbpedia/databus-maven-plugin) [![Maven Central Version](https://img.shields.io/maven-central/v/org.dbpedia.databus/databus-maven-plugin.svg)](https://search.maven.org/search?q=g:org.dbpedia.databus%20AND%20a:databus-maven-plugin&core=gav)
Post `dataid.ttl` into your Databus space to consume your files via the [Databus SPARQL API](http://dev.dbpedia.org/Download_Data)
Feedback: https://forum.dbpedia.org 
 
## User Manual 
Read the frolicking [Manual]( https://github.com/dbpedia/databus-maven-plugin/wiki/User-Manual-v1.3)

## Data Dependencies for Maven 
Our goal is to align the data and software lifecycle. We created a Maven plugin that can upload output of software, e.g. the [DBpedia Information Extraction Framework](https://github.com/dbpedia/extraction-framework/) to the Databus platform. The other tools can include it again, just like software dependencies via Maven Central and Archiva. See the [Databus Client](https://github.com/dbpedia/databus-client) and [Databus Derive Plugin](https://github.com/dbpedia/databus-derive) for data dependencies and automating **software with data**.
The plugin was developed to use the features of the Maven software build automation tool for data releases and metadata generation. Once configured properly (1-3 hours), data can be released and re-released systematically in minutes.


<!-- DBpedia's DataID fulfills 31 of 35 Best Practices from the W3C Data on the Web Best Practices Working Group, cf. [implementation report](http://w3c.github.io/dwbp/dwbp-implementation-report.html) 

<img title="DWBP Implementation Report Summary" width="400" src="https://raw.githubusercontent.com/dbpedia/databus-maven-plugin/master/DWBP.png" >
-->

# Development 

## License
License of the software is AGPL with intended copyleft. We expect that you spend your best effort to commit upstream to make this tool better or at least that your extensions are made available again. 
Any contribution will be merged under the copyright of the DBpedia Association. 

## Development notes
* configuration values taken from Maven are configured in `Properties.scala`, use its 'sub-trait' `Locations.scala` to derive filesystem locations from these and `Parameters.scala` to compute all other values derived from the original Maven properties (refactoring into this separation is not yet complete, but please heed this guidelines for additional   configuration-derived fields nontheless)
* Datafile.scala is a quasi decorator for files, use getInputStream to open any file
* Use the issue tracker, do branches instead of forks (we can give access), we will merge with master

## Super-pom
To set defaults for the plugin we developed a parent `pom.xml` in the super pom folder. 
All other `pom.xml` can use it. 
Devnote: When uploading to archiva, select `pomFile` for pom.xml and leave `generate pom` unchecked

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
# Troubleshooting
## databus-shared-lib / Local install works, travis will show `build error`
databus-shared-lib is a co-developed library. If you update it, travis will fail. 
Travis chaches the shared lib every 6 hours, so you need to flush the cache manually or wait 6 hours


## Download from http://databus.dbpedia.org:8081/repository/ fails, no dependency information available
  Note: this section can be removed after completion of https://github.com/dbpedia/databus-maven-plugin/issues/12 
Possible reason: we have installed a dev archiva for now. Depending on your org's network configuration, code might only be accepted from Maven Central and local/allowed maven repos.
* `[WARNING] The POM for org.dbpedia.databus:databus-maven-plugin:jar:1.0-SNAPSHOT is missing, no dependency information available`
* `Could not resolve dependencies for project org.dbpedia.databus:databus-maven-plugin:maven-plugin:1.0-SNAPSHOT: Failure to find org.dbpedia.databus:databus-shared-lib:jar:0.1.4`

Can potentially fixed by locally installing the shared-lib:
* Download Jar: http://databus.dbpedia.org:8081/#artifact-details-download-content/org.dbpedia.databus/databus-shared-lib/0.1.4
* Install dependency: https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html

Then clone the repo and run `mvn install` which will install the databus-maven-plugin locally


## databus plugin goals are not found after installing the plugin via sources (mvn install)
```
[ERROR] Could not find goal 'metadata' in plugin org.dbpedia.databus:databus-maven-plugin:1.1-SNAPSHOT among available goals -> [Help 1]
org.apache.maven.plugin.MojoNotFoundException: Could not find goal 'metadata' in plugin org.dbpedia.databus:databus-maven-plugin:1.1-SNAPSHOT among available goals 
```
Try to wipe (make a copy of it and then delete the original) your m2 (maven local repository) and then build it again. 
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

## UTF-8 - Encoding Errors in the produced data
On Unix: 
run: `grep "LC_ALL" .*` in your /root/ directory and make sure
```
.bash_profile:export LC_ALL=en_US.UTF-8
.bashrc:export LC_ALL=en_US.UTF-8
```
is set.

## [ERROR] org.scalatra.HaltException at data deploy

Its possible you messed up at creating your webid (especially if you have multiple names in the webid).
Check if your SAN is correct by using
`openssl x509 -in certificate.crt -text -noout`
on your .crt file (if you just have the pkcs12 file use `openssl pkcs12 -in [yourfile.pfx] -clcerts -nokeys -out [drlive.crt]`).
Check if the SAN matches the one you used in the pom.xml and if not change [the cert.config](https://github.com/dbpedia/webid#x509-certificate-with-webid-in-subject-alternative-name) and generate the pkcs12 file again.

