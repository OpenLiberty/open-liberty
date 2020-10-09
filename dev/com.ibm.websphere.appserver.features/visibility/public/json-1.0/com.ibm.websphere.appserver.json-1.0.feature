-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.json-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-API-Package: com.ibm.json.java; type="ibm-api", \
 com.ibm.json.xml; type="ibm-api"
IBM-ShortName: json-1.0
Subsystem-Name: JavaScript Object Notation for Java 1.0
-bundles=com.ibm.json4j
-jars=com.ibm.websphere.appserver.api.json; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.json_1.0-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
