-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.microprofile.rest.client-1.0
singleton=true
-features=com.ibm.websphere.appserver.javax.cdi-1.2; ibm.tolerates:=2.0, \
 com.ibm.websphere.appserver.javax.annotation-1.2; ibm.tolerates:=1.3, \
 com.ibm.websphere.appserver.javax.jaxrs-2.0; ibm.tolerates:=2.1
-bundles=com.ibm.websphere.org.eclipse.microprofile.rest.client.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.rest.client:microprofile-rest-client-api:1.0.1"
kind=ga
edition=core
WLP-Activation-Type: parallel
