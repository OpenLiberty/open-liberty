-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonbContainer-2.0
visibility=public
IBM-API-Package: jakarta.json.bind; type="spec", \
 jakarta.json.bind.adapter; type="spec", \
 jakarta.json.bind.annotation; type="spec", \
 jakarta.json.bind.config; type="spec", \
 jakarta.json.bind.serializer; type="spec", \
 jakarta.json.bind.spi; type="spec"
IBM-ShortName: jsonbContainer-2.0
Subsystem-Name: Jakarta JSON Binding 2.0 Container
-features=io.openliberty.jsonbImpl-2.0.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=com.ibm.ws.jsonb.service
kind=beta
edition=core
WLP-Activation-Type: parallel
