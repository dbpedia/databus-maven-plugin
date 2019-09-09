# Wikipedia Article Categories and categories metadata
Contains the Wikipedia categories per Article and category metadata (hierarchy and labels)

The dataset is split into 3 different types of files. The `_articles` files contain the  Wikipedia categories assigned per article connected via `dct:subject`. The `skos` files partially describe the Wikipedia Category system modelled with the [SKOS vocabulary]. For each category `skos:prefLabel`, parent categories (using `skos:broader`), and `skos:related` categories are extracted. The `labels` categories contain the `rdfs:label` of the category resources. 

