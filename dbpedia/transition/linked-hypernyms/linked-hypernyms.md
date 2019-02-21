# Linked Hypernyms Dataset (LHD)
(Transitional artifact for contributed dataset) The Linked Hypernyms Dataset (LHD) provides entities described by Dutch, English and German Wikipedia articles with types in the DBpedia namespace.

## Description
The dataset provides more complete `rdf:type` based on the Wikipedia abstracts. 
It is a transitional artifact, i.e. the file was contributed by Tomas Kliegr originally. We included the old versions now, but perspectively Tomas Kliegr has to create his own artifact and release the data there in regular intervals. This artifact will be discontinued. 

There are two different versions of the dataset for each language:
* `linked-hypernyms_type=dbo_lang={lang}.ttl.bz2`  //  Most accurate - result of
pattern matching, entity types are from  DBpedia ontology
* `linked-hypernyms_type=ext_lang={lang}.ttl.bz2`  //   Highest coverage and
specificity - merged output of multiple algorithms, entity types are DBpedia resources for most precise types

### Languages supported
* English
* German 
* Dutch

### Attribution fullfilled by

* citing the paper below
* attributing the Czech DBepdia Chapter: https://cs.dbpedia.org/

###  Abstract
Kliegr, Tomas, Linked Hypernyms: Enriching DBpedia with Targeted Hypernym Discovery (2015). Journal of Web Semantics First Look. Available at SSRN: https://ssrn.com/abstract=3199181 or http://dx.doi.org/10.2139/ssrn.3199181 

The Linked Hypernyms Dataset (LHD) provides entities described by Dutch, English and German Wikipedia articles with types in the DBpedia namespace. The types are extracted from the first sentences of Wikipedia articles using Hearst pattern matching over part-of-speech annotated text and disambiguated to DBpedia concepts. The dataset covers 1.3 million RDF type triples from English Wikipedia, out of which 1 million RDF type triples were found not to overlap with DBpedia, and 0.4 million with YAGO2s. There are about 770 thousand German and 650 thousand Dutch Wikipedia entities assigned a novel type, which exceeds the number of entities in the localized DBpedia for the respective language. RDF type triples from the German dataset have been incorporated to the German DBpedia. Quality assessment was performed altogether based on 16.500 human ratings and annotations. For the English dataset, the average accuracy is 0.86, for German 0.77 and for Dutch 0.88. The accuracy of raw plain text hypernyms exceeds 0.90 for all languages. The LHD release described and evaluated in this article targets DBpedia 3.8, LHD version for the DBpedia 3.9 containing approximately 4.5 million RDF type triples is also available.

## Dataset Changelog
### 2019.02.10
We took the data from the old releases  as is. 
