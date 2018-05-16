# Total Interoperability (TI) - Data Release Tool
Release data on your website with total interoperability and push it to repos and services (including the DBpedia Databus)

# License
License of the software and templates is GPL with intended copyleft. We expect that you spend your best effort to commit upstream to make this tool better or at least that your extensions are made available again. 

# Problem
Publishing data on the web in a de-centralised manner is the grand vision of the Semantic Web. However, decentralisation comes with its problems. Just putting data files on your web server and creating a landing page to describe this data, just goes a short way. Humans can read the landing page and use the right-click save-as to download the files. Crawlers can discover links and can download the files automatically, but have no understanding of the context, publisher, version or other metadata of the files, making its usage limited. 

# Primary solution
The TI tools will primarily deliver:

* a template engine to create customizable landing pages for data
* inclusion of machine-readable metadata in JSON-LD in the <meta><script> part of the landing page to allow automatic processing of discovery, download and machine understanding
* notification of data releases to the DBpedia Databus 
* the initial format for data publication is RDF

# Total Interoperability
The tool is supposed to provide *Total Interoperability*, whenever data is published. If the tool is not providing the necessary interoperability for your use case, it is your own responsibility to at least file a feature request in the issue tracker or ideally implement additional formats, features, validity checks and templates. We keep a list of potential add-ons and formats below. 

# Instructions 
*Plan for now*

HTML and Metdata
* provide metadata in JSON-LD using schema.org vocab
* use velocity templates to generate HTML
* implement something to track versioning

Data
* Parse and validate triples and provide an errorlog
* produce checksums
* produce additional formats, e.g. one-line turtle as .bz2
* sign with WebID





# Add-ons and formats
For future reference

## List of versioning approaches

## List of notifications 
* https://www.openarchives.org/rs/toc
* https://www.openarchives.org/rs/notification/1.0.1/notification
* http://aksw.org/Projects/SemanticPingback.html
* https://en.wikipedia.org/wiki/WebSub

## List of repos for upload of metadata
* https://www.sciencebase.gov/catalog/ 





