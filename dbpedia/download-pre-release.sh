#!/bin/bash

endpoint="https://databus.dbpedia.org/repo/sparql"

publisher_generic=""
publisher_mappings="https://databus.dbpedia.org/marvin/mappings"
publisher_wikidata=""

function group_iri {
	case $1 in
		"generic") echo $publisher_generic;;
		"mappings") echo $publisher_mappings;;
		"wikidata") echo $publisher_wikidata;;
		*) echo "";;
	esac
}

query="PREFIX dataid: <http://dataid.dbpedia.org/ns/core#>
PREFIX dct: <http://purl.org/dc/terms/>
PREFIX dcat:  <http://www.w3.org/ns/dcat#>

SELECT ?dataid {
  ?dataid dataid:version ?latest .
  {
    SELECT (max(?version) as ?latest) {
      ?dataset dataid:artifact <$(group_iri $1)/$2> .
      ?dataset dataid:version ?version .
    } group by ?artifact
  }
}"

if [ -z $(group_iri $1) ]; then
  echo -e "#\n# release for $1 not found \n#"
  exit 1
fi

response=$(curl -s \
  --data-urlencode format=text/tab-separated-values \
  --data-urlencode query="$query" "$endpoint")

req_err=$(echo $response | grep "Virtuoso\|Error\SPARQL")

c_downloaded=0
c_skipped=0

function download {
  for dataid in $*
  do
  	# get file locations from dataid
  	paths=`rapper -i turtle -o ntriples $dataid 2> /dev/null \
  	| grep "<http://www.w3.org/ns/dcat#downloadURL>" \
  	| cut -d" " -f3 \
  	| sed "s/[<>]//g"`

  	for fileurl in $paths
  	do
  		# get artifactId, version and filename
  		filename=${fileurl##*/}
  		version=${fileurl%/*}
  		artifactId=${version%/*}
  		version=${version##*/}
  		artifactId=${artifactId##*/} # echo $artifactId $version

  		# mkdirs
  		mkdir -p $version

  		# download
      if [ ! -f $version/$filename ]; then
        echo "downloading $fileurl"
        curl -o $version/$filename $fileurl
        c_downloaded=$[$c_downloaded+1]
      else
        echo "skipping $fileurl"
        c_skipped=$[$c_skipped+1]
      fi
  		#wget -O - $fileurl 2> /dev/null \
  		#| lbzcat \
  		#| rapper -i ntriples - http://baseuri 2> /dev/null \
  		#| lbzip2 > $artifactId/$version/$filename
  	done
    echo -e "#\n# downloaded: $c_downloaded "
    echo -e "# skipped: $c_skipped \n#"
  done
}

if [ -z "$req_err" ]; then
  download  $(echo "$response" | tail -n+2 | sed "s/\"//g")
else
  >&2 echo "$response"
fi
