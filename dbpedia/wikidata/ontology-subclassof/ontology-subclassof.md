# Wikidata Ontology SubclassOf
Ontology Parent Classes


Captures all subclasses of wikidata based on P279? 

Preproccessing
Used internally to improve owl:equivalence class mappings and R2R
exactly as found in wikidata at the time of extraction

move to instance types
```
 "P31": [
        {
            "rdf:type": "$getDBpediaClass"
        }
],

```
if finds a match, then takes the lowest class for typing
