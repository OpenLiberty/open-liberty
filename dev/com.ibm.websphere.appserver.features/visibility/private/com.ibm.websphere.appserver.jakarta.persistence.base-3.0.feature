-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jakarta.persistence.base-3.0
singleton=true
IBM-Process-Types: server, \
 client
-bundles=com.ibm.ws.jakartaee.persistence.3.0; location:=lib/
kind=noship
edition=core
WLP-Activation-Type: parallel
