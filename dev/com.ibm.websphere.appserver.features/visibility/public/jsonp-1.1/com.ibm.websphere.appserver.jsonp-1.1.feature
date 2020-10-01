-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsonp-1.1
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-API-Package: javax.json; type="spec", \
 javax.json.stream; type="spec", \
 javax.json.spi; type="spec"
IBM-ShortName: jsonp-1.1
Subsystem-Name: JavaScript Object Notation Processing 1.1
-features=com.ibm.websphere.appserver.jsonpInternal-1.1
kind=ga
edition=core
WLP-Activation-Type: parallel
