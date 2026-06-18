-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonpContainer-2.2
visibility=public
singleton=true
IBM-API-Package: jakarta.json; type="spec", \
 jakarta.json.stream; type="spec", \
 jakarta.json.spi; type="spec"
IBM-ShortName: jsonpContainer-2.2
Subsystem-Name: Jakarta JSON Processing 2.2 Container
-features=io.openliberty.jsonpImpl-2.2.0, \
  com.ibm.websphere.appserver.eeCompatible-12.0
kind=noship
edition=full
WLP-Activation-Type: parallel
WLP-Platform: jakartaee-12.0
