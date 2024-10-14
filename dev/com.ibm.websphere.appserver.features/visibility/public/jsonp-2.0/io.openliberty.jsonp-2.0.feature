-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonp-2.0
visibility=public
singleton=true
IBM-API-Package: jakarta.json; type="spec", \
 jakarta.json.stream; type="spec", \
 jakarta.json.spi; type="spec"
IBM-ShortName: jsonp-2.0
Subsystem-Name: Jakarta JSON Processing 2.0
-features=com.ibm.websphere.appserver.eeCompatible-9.0, \
  io.openliberty.jsonpInternal-2.0
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: jakartaee-9.1
