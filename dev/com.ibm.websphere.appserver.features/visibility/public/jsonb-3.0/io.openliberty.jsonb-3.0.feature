-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonb-3.0
visibility=public
singleton=true
IBM-ShortName: jsonb-3.0
Subsystem-Name: Jakarta JSON Binding 3.0
IBM-API-Package: jakarta.json.bind; type="spec", \
 jakarta.json.bind.adapter; type="spec", \
 jakarta.json.bind.annotation; type="spec", \
 jakarta.json.bind.config; type="spec", \
 jakarta.json.bind.serializer; type="spec", \
 jakarta.json.bind.spi; type="spec"
-features=com.ibm.websphere.appserver.eeCompatible-10.0, \
  io.openliberty.jsonbInternal-3.0
-bundles=com.ibm.ws.jsonb.service
kind=noship
edition=full
WLP-Activation-Type: parallel
