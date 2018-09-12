#! /bin/bash

set -euo pipefail

# TODO better cli

for dataid in $*
do 
    echo "downloading: $dataid"
	# get dowload paths for dataid
	paths=`rapper -i turtle -o ntriples $dataid 2> /dev/null \
	| grep "<http://www.w3.org/ns/dcat#downloadURL>" \
	| cut -d" " -f3 \
	| sed "s/[<>]//g"`

	for fileurl in $paths
	do
		# get artifactId , version and filename
		filename=${fileurl##*/}
		version=${fileurl%/*} 
		artifactId=${version%/*}
		version=${version##*/} 
		artifactId=${artifactId##*/} # echo $artifactId $version

		# mkdirs
		mkdir -p $artifactId/$version

		# download
		curl --fail --silent --show-error $fileurl 2> /dev/null \
		| lbzcat \
		| rapper -i ntriples - http://baseuri 2> /dev/null \
		| lbzip2 > $artifactId/$version/$filename
	done
done
