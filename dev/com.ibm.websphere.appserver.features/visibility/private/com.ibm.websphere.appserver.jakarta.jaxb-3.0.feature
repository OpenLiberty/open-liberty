-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jakarta.jaxb-3.0
singleton=true
-features=com.ibm.websphere.appserver.jakarta.activation-2.0; apiJar=false
-bundles=\
 io.openliberty.jakarta.jaxb.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.xml.bind:jakarta.xml.bind-api:3.0.0"
kind=noship
edition=full
WLP-Activation-Type: parallel
