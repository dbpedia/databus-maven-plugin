
# Geo-coordinates extracted with mappings  
Contains geographic coordinates from the Wikipedia Infoboxes refined by the mapping-based extraction.  
  
The dataset contains all triples extracted with the help of the [Geocoordinates Mappings](http://mappings.dbpedia.org/index.php/Template:GeocoordinatesMapping). Whereas [generic geo coordinates datasets](https://databus.dbpedia.org/dbpedia/generic/geo-coordinates) spots any geocoordinate in an infobox without contextualizing it, the mappings allow to describe which kind of location the coordinates are describing. This can be the coordinates of the actual location of the resource  itself
  

    <http://dbpedia.org/resource/Atlantic_Ocean> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing> .  
    <http://dbpedia.org/resource/Atlantic_Ocean> <http://www.w3.org/2003/01/geo/wgs84_pos#lat> "0.0"^^<http://www.w3.org/2001/XMLSchema#float> .  
    <http://dbpedia.org/resource/Atlantic_Ocean> <http://www.w3.org/2003/01/geo/wgs84_pos#long> "-25.0"^^<http://www.w3.org/2001/XMLSchema#float> .  
    <http://dbpedia.org/resource/Atlantic_Ocean> <http://www.georss.org/georss/point> "0.0 -25.0" .  

  
but also coordinates of locations associated with the resource (e.g. the resting place of Alfred Nobel) 
  

    <http://dbpedia.org/resource/Alfred_Nobel> <http://dbpedia.org/ontology/restingPlacePosition> <http://dbpedia.org/resource/Alfred_Nobel__restingPlacePosition__1> .  
    <http://dbpedia.org/resource/Alfred_Nobel__restingPlacePosition__1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing> .  
    <http://dbpedia.org/resource/Alfred_Nobel__restingPlacePosition__1> <http://www.w3.org/2003/01/geo/wgs84_pos#lat> "59.356811111111114"^^<http://www.w3.org/2001/XMLSchema#float> .  
    <http://dbpedia.org/resource/Alfred_Nobel__restingPlacePosition__1> <http://www.w3.org/2003/01/geo/wgs84_pos#long> "18.01928611111111"^^<http://www.w3.org/2001/XMLSchema#float> .  
    <http://dbpedia.org/resource/Alfred_Nobel__restingPlacePosition__1> <http://www.georss.org/georss/point> "59.356811111111114 18.01928611111111" .  

  
You can have a look at the mappings used for [Alfred Nobel (Person)](http://mappings.dbpedia.org/index.php/Mapping_en:Infobox_person) and [Atlantic Ocean (body of water)](http://mappings.dbpedia.org/index.php/Mapping_en:Infobox_body_of_water) .