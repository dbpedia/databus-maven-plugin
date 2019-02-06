# Wikidata Instance Types
Contains triples of the form $object rdf:type $class 

Using P31 as rdf:type . 
If the typed class is in in the DBpedia Ontology, it will be used, otherwise the type is discarded. 
E.g. 

```
<QXYZ> <P31> <Q5> . # Q5 is Person
<Q5> owl:equivalentClass dbo:Person .
------------------------
<QXYZ> rdf:type dbo:Person .
```
Function used:
```
 "P31": [
        {
            "rdf:type": "$getDBpediaClass"
        }
],

```

The extractor uses the data from ontology-subclass-of artifact to optimize the hierarchy and enrich equivalent classes.

The mappings between Wikidata Items and classes can be edited in the [Mappings Wiki](http://mappings.dbpedia.org/index.php/OntologyClass:Person)

* https://github.com/dbpedia/extraction-framework/blob/master/core/src/main/resources/wikidatar2r.json#L92
* https://github.com/dbpedia/extraction-framework/blob/master/core/src/main/resources/wikidatar2r.json#L87


