-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsonpContainer-1.1
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-API-Package: javax.json; type="spec", \
 javax.json.stream; type="spec", \
 javax.json.spi; type="spec"
IBM-ShortName: jsonpContainer-1.1
Subsystem-Name: JavaScript Object Notation Processing 1.1 via Bells
-features=com.ibm.websphere.appserver.jsonpImpl-1.1.0
kind=ga
edition=core
