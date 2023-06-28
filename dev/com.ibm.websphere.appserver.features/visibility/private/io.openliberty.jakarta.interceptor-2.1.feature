-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.interceptor-2.1
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-10.0; ibm.tolerates:="11.0"
-bundles=io.openliberty.jakarta.interceptor.2.1; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.interceptor:jakarta.interceptor-api:2.1.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
