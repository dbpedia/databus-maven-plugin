# Troubleshooting
## databus-shared-lib / Local install works, travis will show `build error`
databus-shared-lib is a co-developed library. If you update it, travis will fail. 
Travis chaches the shared lib every 6 hours, so you need to flush the cache manually or wait 6 hours


## Download from http://databus.dbpedia.org:8081/repository/ fails, no dependency information available
  Note: this section can be removed after completion of https://github.com/dbpedia/databus-maven-plugin/issues/12 
Possible reason: we have installed a dev archiva for now. Depending on your org's network configuration, code might only be accepted from Maven Central and local/allowed maven repos.
* `[WARNING] The POM for org.dbpedia.databus:databus-maven-plugin:jar:1.0-SNAPSHOT is missing, no dependency information available`
* `Could not resolve dependencies for project org.dbpedia.databus:databus-maven-plugin:maven-plugin:1.0-SNAPSHOT: Failure to find org.dbpedia.databus:databus-shared-lib:jar:0.1.4`

Can potentially fixed by locally installing the shared-lib:
* Download Jar: http://databus.dbpedia.org:8081/#artifact-details-download-content/org.dbpedia.databus/databus-shared-lib/0.1.4
* Install dependency: https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html

Then clone the repo and run `mvn install` which will install the databus-maven-plugin locally


## databus plugin goals are not found after installing the plugin via sources (mvn install)
```
[ERROR] Could not find goal 'metadata' in plugin org.dbpedia.databus:databus-maven-plugin:1.1-SNAPSHOT among available goals -> [Help 1]
org.apache.maven.plugin.MojoNotFoundException: Could not find goal 'metadata' in plugin org.dbpedia.databus:databus-maven-plugin:1.1-SNAPSHOT among available goals 
```
Try to wipe (make a copy of it and then delete the original) your m2 (maven local repository) and then build it again. 
## BUILD FAILURE, no mojo-descriptors found (when using `mvn install` to install the databus-maven-plugin)
This is most likely caused by using an old maven version (observed in version `3.0.5`)
A workaround for this would be replacing:
```
<plugin>
	<groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-plugin-plugin</artifactId>
	<version>3.4</version>
</plugin>
```
with
```
<plugin>
	<groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-plugin-plugin</artifactId>
	<version>3.4</version>
        <configuration>
		<skipErrorNoDescriptorsFound>true</skipErrorNoDescriptorsFound>
	</configuration>
        <executions>
		<execution>
		        <id>mojo-descriptor</id>
		        <goals>
                            <goal>descriptor</goal>
                        </goals>
		</execution>
	</executions>
</plugin>

``` 
in `databus-maven-plugin/pom.xml`

## UTF-8 - Encoding Errors in the produced data
On Unix: 
run: `grep "LC_ALL" .*` in your /root/ directory and make sure
```
.bash_profile:export LC_ALL=en_US.UTF-8
.bashrc:export LC_ALL=en_US.UTF-8
```
is set.

## [ERROR] org.scalatra.HaltException at data deploy

Its possible you messed up at creating your webid (especially if you have multiple names in the webid) and used the wrong SAN in the cert.config.\
Check if your SAN is correct by using\
`openssl x509 -in certificate.crt -text -noout`\
on your .crt file.\
If you just have the pkcs12 file use 
`openssl pkcs12 -in [yourfile.pfx] -clcerts -nokeys -out [drlive.crt]`\
to generate it.\
Check if the SAN matches the one you used in the webid.ttl and if not change the [cert.config](https://github.com/dbpedia/webid#x509-certificate-with-webid-in-subject-alternative-name) and generate the pkcs12 file again.
