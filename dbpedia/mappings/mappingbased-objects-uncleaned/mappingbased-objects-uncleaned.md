# (Uncleaned) Object properties extracted with mappings
Uncleand High quality statements with IRI object values extracted by the mappings extraction from Wikipedia Infoboxes. 

Offers complementary statements (Entity-to-Entity relations) from Wikipedia Infoboxes to [mappingbased-literals](https://databus.dbpedia.org/dbpedia/${project.groupId}/mappingbased-literals/${project.version}) 

NOTE: There also is a [cleaned version](https://databus.dbpedia.org/dbpedia/${project.groupId}/mappingbased-objects/${project.version}) of this dataset available: 


Uncleaned means that two post-processing steps are *not* performed on this dataset:
* type consistency check, i.e. type of object matches the range of property
* redirecting of objects, i.e. http://dbpedia.org/resource/Billl_Clinton to Bill Clinton
