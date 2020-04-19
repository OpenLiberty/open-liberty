-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsonb-2.0
visibility=public
IBM-ShortName: jsonb-2.0
Subsystem-Name: JavaScript Object Notation Binding 2.0
IBM-API-Package: jakarta.json.bind; type="spec", \
 jakarta.json.bind.adapter; type="spec", \
 jakarta.json.bind.annotation; type="spec", \
 jakarta.json.bind.config; type="spec", \
 jakarta.json.bind.serializer; type="spec", \
 jakarta.json.bind.spi; type="spec"
-features=com.ibm.websphere.appserver.jsonbInternal-2.0
-bundles=com.ibm.ws.jsonb.service
kind=noship
edition=full
WLP-Activation-Type: parallel
