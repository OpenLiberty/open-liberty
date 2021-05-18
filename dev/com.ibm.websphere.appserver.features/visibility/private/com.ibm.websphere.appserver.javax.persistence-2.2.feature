-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.persistence-2.2
WLP-DisableAllFeatures-OnConflict: false
singleton=true
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.containerServices-1.0, \
  com.ibm.websphere.appserver.javax.persistence.base-2.2, \
  com.ibm.websphere.appserver.eeCompatible-8.0; ibm.tolerates:="6.0,7.0"
-bundles=com.ibm.ws.javaee.persistence.api.2.2
-jars=com.ibm.websphere.javaee.persistence.2.2; location:=dev/api/spec/; mavenCoordinates="javax.persistence:javax.persistence-api:2.2"
kind=ga
edition=core
WLP-Activation-Type: parallel
