# Mappingbased Objects uncleaned                    
High-quality data extracted using the mapping-based extraction (Object properties only).

The dump contains only dbo properties, which are mapped by this json file, using patterns:
https://github.com/dbpedia/extraction-framework/blob/master/core/src/main/resources/wikidatar2r.json


If there is an owl:equivalentProperty from a Wikidata property to a DBO property, the property is replaced.
Errors from here are recorded in the debug artifact (object vs dataype)

Improvement can be most effectively done by adding more equivalent property mappings to http://mapping.dbpedia.org
* https://github.com/dbpedia/extraction-framework/blob/master/core/src/main/scala/org/dbpedia/extraction/mappings/wikidata/WikidataR2RExtractor.scala
