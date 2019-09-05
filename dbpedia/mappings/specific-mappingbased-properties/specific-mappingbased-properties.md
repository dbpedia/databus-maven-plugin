# Numeric Literals converted to designated units with class-specific property mappings
Infobox numerical data from the mappings extraction using units of measurement more convenient for the resource class/type.

The triples in [mappingbased-literals](https://databus.dbpedia.org/dbpedia/${project.groupId}/mappingbased-literals) use normalized values according to the base unit for the property (see  [docu](https://databus.dbpedia.org/dbpedia/${project.groupId}/mappingbased-literals/${project.version}) for more details). However, this dataset contains triples where the values are converted to a specific unit of measurement more convenient for the resource class (e.g. square kilometres instead of square metres for the area of a city or runtime of a movie in minutes instead of seconds). To distinguish between the base unit values from [mappingbased-literals](https://databus.dbpedia.org/dbpedia/${project.groupId}/mappingbased-literals), specific properties of the form `http://dbpedia.org/ontology/$className/$propertyName`. The target conversion unit is retrieved via specified datatype of `rdfs:range` of the specific property (see e.g. [runtime]()).


http://mappings.dbpedia.org/index.php?title=Special:AllPages&namespace=202
