for i in `cat files.list` ; do echo -n "rename 's/"$i/ ; echo  -n $i | sed 's/\.//g;s/-//g;s/_//g;s/^/links_domain=/;s/$/_lang=en.nt.bz2/' ;  echo "/' */dbpedia.org/*  " ; done > rename_en_gen.sh


for i in `ls` ; do 
	mv $i/dbpedia.org/* $i/
done 
