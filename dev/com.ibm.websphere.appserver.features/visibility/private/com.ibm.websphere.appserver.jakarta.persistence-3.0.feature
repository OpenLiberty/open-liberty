-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jakarta.persistence-3.0
singleton=true
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.jakarta.persistence.base-3.0
-bundles=com.ibm.ws.jakartaee.persistence.api.3.0
-jars=com.ibm.websphere.jakartaee.persistence.3.0; location:=dev/api/spec/; mavenCoordinates="jakarta.persistence:jakarta.persistence-api:3.0-RC1"
kind=noship
edition=core
WLP-Activation-Type: parallel
