-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsonb-1.0
visibility=public
IBM-ShortName: jsonb-1.0
Subsystem-Name: JavaScript Object Notation Binding 1.0
IBM-API-Package: javax.json.bind; type="spec", \
 javax.json.bind.adapter; type="spec", \
 javax.json.bind.annotation; type="spec", \
 javax.json.bind.config; type="spec", \
 javax.json.bind.serializer; type="spec", \
 javax.json.bind.spi; type="spec"
-features=com.ibm.websphere.appserver.jsonbInternal-1.0
-bundles=com.ibm.ws.jsonb.service
kind=ga
edition=core
WLP-Activation-Type: parallel
