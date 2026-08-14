-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonp-2.2
visibility=public
singleton=true
IBM-API-Package: jakarta.json; type="spec", \
 jakarta.json.stream; type="spec", \
 jakarta.json.spi; type="spec"
IBM-ShortName: jsonp-2.2
Subsystem-Name: Jakarta JSON Processing 2.2
-features=com.ibm.websphere.appserver.eeCompatible-12.0, \
  io.openliberty.jsonpInternal-2.2
kind=noship
edition=full
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: jakartaee-12.0
