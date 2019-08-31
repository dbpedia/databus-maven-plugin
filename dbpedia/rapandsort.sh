#!/bin/bash

ARTIFACT=$1


if [ ! -d "tmpfolder" ]; then
    mkdir -p tmpfolder
fi

# skip parent
if [ "$ARTIFACT" = "common-metadata" ]; then
    exit
fi

LOG=tmpfolder/logfile
echo -n "" > $LOG

TMPFILE=tmpfolder/tmpfile

# paths are now relative to the artifact dir
for file in src/main/databus/*/*.ttl.bz2; do
        echo "processing $file ..." >> $LOG  ;
        lbzip2 -dc $file |\
	rapper -i ntriples -O - - file 2>>$LOG |\
	#ascii2uni -a U 2>>$LOG  |\ 1!! this breaks encoding and IRIs 
	LC_ALL=C sort --parallel=4 -u -T tmpfolder  |\
        lbzip2 > $TMPFILE;
        echo "finished processing $file" >> $LOG ;
        mv $TMPFILE $file;
done
