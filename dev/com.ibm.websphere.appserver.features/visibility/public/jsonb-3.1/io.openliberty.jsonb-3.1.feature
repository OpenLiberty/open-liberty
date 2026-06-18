-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonb-3.1
visibility=public
singleton=true
IBM-ShortName: jsonb-3.1
Subsystem-Name: Jakarta JSON Binding 3.1
IBM-API-Package: jakarta.json.bind; type="spec", \
 jakarta.json.bind.adapter; type="spec", \
 jakarta.json.bind.annotation; type="spec", \
 jakarta.json.bind.config; type="spec", \
 jakarta.json.bind.serializer; type="spec", \
 jakarta.json.bind.spi; type="spec"
-features=com.ibm.websphere.appserver.eeCompatible-12.0, \
  io.openliberty.jsonbInternal-3.1
-bundles=com.ibm.ws.jsonb.service
kind=noship
edition=full
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: jakartaee-12.0
