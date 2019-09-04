# DBpedia Globald IDs Assignment Snapshot
Materialized view of sameAs clustering for entities and the singleton and cluster ids assigned to them by DBpedia ID Management.

The Global IDs assignment is stored as tabular representation in TSV format with 3 columns: original (local) IRI;  the stable singleton ID in base58 ; and the cluster ID in base58. For more information visit [http://dev.dbpedia.org/ID_and_Clustering](http://dev.dbpedia.org/ID_and_Clustering).
Note: The DBpedia Global ID of an IRI can be derived by appending the base58 cluster ID of it to the string `https://global.dbpedia.org/id/`.

# Changelog
## 2019.08.07
* included musicbrainz identifiers for existing links from Wikidata

## 2019.02.28
* initial release with Wikidata and DBpedia identifiers 
