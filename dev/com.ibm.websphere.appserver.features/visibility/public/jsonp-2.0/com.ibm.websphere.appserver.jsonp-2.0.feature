-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsonp-2.0
visibility=public
singleton=true
IBM-API-Package: jakarta.json; type="spec", \
 jakarta.json.stream; type="spec", \
 jakarta.json.spi; type="spec"
IBM-ShortName: jsonp-2.0
Subsystem-Name: JavaScript Object Notation Processing 2.0
-features=com.ibm.websphere.appserver.jsonpInternal-2.0
kind=noship
edition=full
WLP-Activation-Type: parallel
