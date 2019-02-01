# Wikidata Alias
Wikidata-specific dataset containing aliases for languages in the mappings wiki. Aliases for languages not in the mapping wiki are found in the _nmw variant.

Aliases are alternative names for items that are placed in the Also known as column of the table on top of every Wikidata item page.
From https://www.wikidata.org/wiki/Help:Aliases
There can be several aliases for each item, but only one label (other dataset).

## Issues
Currently data is grouped in two categories:
* aliases from mapping wiki languages (around 40)
* aliases from all other languages
In the future, we might separate these into one file per language, which will increase the number of files from 2 (nmw as content variant) now, to many (basically using iso codes as content variants, beyond the 120 wikipedia versions)
