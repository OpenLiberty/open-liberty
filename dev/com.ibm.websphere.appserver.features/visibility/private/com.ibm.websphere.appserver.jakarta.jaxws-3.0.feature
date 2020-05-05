-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jakarta.jaxws-3.0
singleton=true
-features=com.ibm.websphere.appserver.jakarta.jws-3.0; apiJar=false,\
 com.ibm.websphere.appserver.jakarta.jaxb-3.0; apiJar=false
-bundles=io.openliberty.jakarta.jaxws.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.xml.ws:jakarta.xml.ws-api:3.0.0"
kind=noship
edition=full
WLP-Activation-Type: parallel
