-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsonbContainer-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-API-Package: javax.json.bind; type="spec", \
 javax.json.bind.adapter; type="spec", \
 javax.json.bind.annotation; type="spec", \
 javax.json.bind.config; type="spec", \
 javax.json.bind.serializer; type="spec", \
 javax.json.bind.spi; type="spec"
IBM-ShortName: jsonbContainer-1.0
Subsystem-Name: JavaScript Object Notation Binding 1.0 via Bells
-features=com.ibm.websphere.appserver.jsonbImpl-1.0.0
-bundles=com.ibm.ws.jsonb.service
kind=ga
edition=core
