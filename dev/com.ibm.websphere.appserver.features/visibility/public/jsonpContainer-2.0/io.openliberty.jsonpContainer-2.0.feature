-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonpContainer-2.0
visibility=public
IBM-API-Package: jakarta.json; type="spec", \
 jakarta.json.stream; type="spec", \
 jakarta.json.spi; type="spec"
IBM-ShortName: jsonpContainer-2.0
Subsystem-Name: Jakarta JSON Processing 2.0 Container
-features=io.openliberty.jsonpImpl-2.0.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0
kind=beta
edition=core
WLP-Activation-Type: parallel
