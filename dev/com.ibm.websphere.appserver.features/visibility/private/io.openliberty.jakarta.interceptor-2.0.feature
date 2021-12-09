-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.interceptor-2.0
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-9.0; ibm.tolerates:="10.0"
-bundles=io.openliberty.jakarta.interceptor.2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.interceptor:jakarta.interceptor-api:2.0.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
