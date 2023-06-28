-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonp-2.1
visibility=public
singleton=true
IBM-API-Package: jakarta.json; type="spec", \
 jakarta.json.stream; type="spec", \
 jakarta.json.spi; type="spec"
IBM-ShortName: jsonp-2.1
Subsystem-Name: Jakarta JSON Processing 2.1
-features=com.ibm.websphere.appserver.eeCompatible-10.0; ibm.tolerates:="11.0", \
  io.openliberty.jsonpInternal-2.1
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
