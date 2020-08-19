# Databus Maven Plugin 
[![Build Status](https://travis-ci.org/dbpedia/databus-maven-plugin.svg?branch=master)](https://travis-ci.org/dbpedia/databus-maven-plugin) [![Maven Central Version](https://img.shields.io/maven-central/v/org.dbpedia.databus/databus-maven-plugin.svg)](https://search.maven.org/search?q=g:org.dbpedia.databus%20AND%20a:databus-maven-plugin&core=gav)

Databus is a Digital Factory Platform and transforms data pipelines into data networks. 
Our goal is to align the data and software lifecycle. We created a Maven plugin that can upload output of software, 
e.g. the [DBpedia Information Extraction Framework](https://github.com/dbpedia/extraction-framework/) to the Databus platform. 
The other tools can include it again, just like software dependencies via Maven Central and Archiva. 
See the [Databus Client](https://github.com/dbpedia/databus-client) and [Databus Derive Plugin](https://github.com/dbpedia/databus-derive) for data dependencies and automating **software with data**.
The plugin was developed to use the features of the Maven software build automation tool for data releases and metadata generation. 
Once configured properly (1-3 hours), data can be released and re-released systematically in minutes.

The databus-maven-plugin is the current CLI to generate metadata about datasets and then upload this metadata, 
so that anybody can query, download, derive and build applications with this data via the [Databus](http://dev.dbpedia.org/Databus).
Databus Maven Plugin posts `dataid.ttl` into your Databus space allowing you to consume your files via the [Databus SPARQL API](http://dev.dbpedia.org/Download_Data) 
It follows a strict Knowledge Engineering methodology, so in comparison to BigData, 
Data Science and any Git-for-data, we implemented well-engineered processes that are repeatable, 
transparent and have a favourable cost-benefit ratio: 

* repeatable: data is uploaded using a modified software methodology from Maven and Maven Central; loading can be configured like a data dependency, so your software will not break
* transparent: uploaders sign their metadata, no license unclarity, provenance on dataset-version-level

Feedback: https://forum.dbpedia.org 
 
## How to 

Read the quick start [guide](docs/UserNotes.md#quick-start) and the [user documentation](docs/UserNotes.md).

## Security notice

* We are currently using a combination of [DBpedia Keycloak Single Sign On](https://databus.dbpedia.org/auth/realms/databus/protocol/openid-connect/registrations?client_id=website&response_type=code&scope=openidemail&redirect_uri=https://databus.dbpedia.org&kc_locale=en) for user registration and front end service and [WebID](https://github.com/dbpedia/webid) profiles with private keys for file and DataID signing. 
* The main problem we are facing is that via the Databus we are trying to automate all processes on servers with cronjobs. Therefore, they require either private keys with no passwords or to add the password to a config file. All of our servers are shared between multiple root developers, so honestly all of the Databus developers have access to all other private keys and accounts. We are investigating how to manage signatures either client-side via browser certificate, or by integrating them into our https://www.keycloak.org/ server, which we also use for https://forum.dbpedia.org
* as a take-away message: yes, WebID is secure, if the space where you host the `.ttl` is secure and if you have good local security and are the only root admin or trust all other admins.

## License

License of the software is AGPL with intended copyleft. We expect that you spend your best effort to commit upstream to make this tool better or at least that your extensions are made available again. 
Any contribution will be merged under the copyright of the DBpedia Association.

## Development 

see [Developer notes](docs/DeveloperNotes.md)

## Troubleshooting

see [here](docs/Troubleshooting.md)

## Linked projects

Development of the plugin started with the idea to load data into software as data dependencies, in a similar manner that Maven can load software dependencies automatically via Maven Central and Archiva. 
Initially, we started with this plugin and were implementing all the features here. 
Now the project has grown into [Databus Website](http://databus.dbpedia.org), [Databus Mods](http://dev.dbpedia.org/Databus_Mods), and [Databus Client](http://dev.dbpedia.org/Databus_Client) as well. 
Therefore we are slimming down this plugin in the following manner:

* `dataid.ttl` stays stable as an interface. If the Databus Maven Plugin is thinner, it is easier to implement in other programming languages.
* file statistics will be calculated by Mods, see [Databus Download](http://dev.dbpedia.org/Download_Data) for a detailed `dataid.ttl` description (also how to query it) and a list of planned changes, as they might break queries. 
* Mods also take the role of validating and testing files
* A lot of format interoperability and conversions issues are now covered via the [Download Client](http://dev.dbpedia.org/Databus_Client)
* License of the client might become more permissive soon -> Apache
