
# xxx.dbpedia.org

rename 's/lobid.org_links/links_domain=lobidorg_lang=de/'   */xxx.dbpedia.org/de/lobid.org_links.nt.bz2
rename 's/dnb_links/links_domain=dnb_lang=de/'   */xxx.dbpedia.org/de/dnb_links.nt.bz2                     
rename 's/vocabulary.wolterskluwer.de-courts_links/links_domain=vocabularywolterskluwerde_courts_lang=de/'   */xxx.dbpedia.org/de/vocabulary.wolterskluwer.de-courts_links.nt.bz2                     
rename 's/vocabulary.wolterskluwer.de-arbeitsrecht_links/links_domain=vocabularywolterskluwerde_arbeitsrecht_lang=de/'   */xxx.dbpedia.org/de/vocabulary.wolterskluwer.de-arbeitsrecht_links.nt.bz2
rename 's/dati.camera.it_links/links_domain=daticamerait_lang=it/'   */xxx.dbpedia.org/it/dati.camera.it_links.nt.bz2
rename 's/geonames.jp_links/links_domain=geonamesjp_lang=ja/'   */xxx.dbpedia.org/ja/geonames.jp_links.nt.bz2
rename 's/test.rce.rnatoolset.net_links/links_domain=testrcernatoolsetnet_lang=nl/'   */xxx.dbpedia.org/nl/test.rce.rnatoolset.net_links.nt.bz2

for i in `ls` ; do 
mv $i/xxx.dbpedia.org/de/* $i/
mv $i/xxx.dbpedia.org/ja/* $i/
mv $i/xxx.dbpedia.org/it/* $i/
mv $i/xxx.dbpedia.org/nl/* $i/
rm -r $i/xxx.dbpedia.org
done 
