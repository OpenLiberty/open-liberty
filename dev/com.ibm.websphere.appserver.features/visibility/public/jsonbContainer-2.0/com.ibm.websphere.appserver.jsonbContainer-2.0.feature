-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsonbContainer-2.0
visibility=public
IBM-API-Package: jakarta.json.bind; type="spec", \
 jakarta.json.bind.adapter; type="spec", \
 jakarta.json.bind.annotation; type="spec", \
 jakarta.json.bind.config; type="spec", \
 jakarta.json.bind.serializer; type="spec", \
 jakarta.json.bind.spi; type="spec"
IBM-ShortName: jsonbContainer-2.0
Subsystem-Name: JavaScript Object Notation Binding 2.0 via Bells
-features=com.ibm.websphere.appserver.jsonbImpl-2.0.0
-bundles=com.ibm.ws.jsonb.service
kind=noship
edition=full
