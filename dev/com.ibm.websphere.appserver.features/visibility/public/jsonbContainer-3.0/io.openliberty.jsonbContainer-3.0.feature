-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonbContainer-3.0
visibility=public
singleton=true
IBM-API-Package: jakarta.json.bind; type="spec", \
 jakarta.json.bind.adapter; type="spec", \
 jakarta.json.bind.annotation; type="spec", \
 jakarta.json.bind.config; type="spec", \
 jakarta.json.bind.serializer; type="spec", \
 jakarta.json.bind.spi; type="spec"
IBM-ShortName: jsonbContainer-3.0
Subsystem-Name: Jakarta JSON Binding 3.0 Container
-features=io.openliberty.jsonbImpl-3.0.0, \
  com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=com.ibm.ws.jsonb.service
kind=noship
edition=full
WLP-Activation-Type: parallel
