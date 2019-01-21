#!/bin/bash

ARTI=$1

if [ ! -d "tmpfolder" ]; then
    mkdir -p tmpfolder
fi

if [ "$ARTI" = "common-metadata" ]; then
    exit
fi

LOG=tmpfolder/logfile
echo -n "" > $LOG

for file in */src/main/databus/*/*.ttl.bz2; do
        echo "processing $file ..." >> $LOG  ;
        lbzip2 -dc $file |\
	rapper -i ntriples -O - - file 2>>$LOG |\
	ascii2uni -a U 2>>$LOG  |\
	LC_ALL=C sort --parallel=4 -u -T tmpfolder  |\
        lbzip2 > tmpfile;
        echo "finished processing $file" >> $LOG ;
        mv tmpfile $file;
done
