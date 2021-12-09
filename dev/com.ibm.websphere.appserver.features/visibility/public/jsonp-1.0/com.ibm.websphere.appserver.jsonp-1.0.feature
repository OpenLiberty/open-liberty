-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsonp-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-API-Package: javax.json; type="spec", \
 javax.json.stream; type="spec", \
 javax.json.spi; type="spec"
IBM-ShortName: jsonp-1.0
Subsystem-Name: JavaScript Object Notation Processing 1.0
-features=com.ibm.websphere.appserver.jsonpImpl-1.0.0, \
  com.ibm.websphere.appserver.eeCompatible-7.0; ibm.tolerates:="8.0, 6.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
