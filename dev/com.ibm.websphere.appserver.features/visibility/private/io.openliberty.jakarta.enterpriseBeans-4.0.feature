-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.enterpriseBeans-4.0
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-9.0; ibm.tolerates:="10.0"
-bundles=io.openliberty.jakarta.enterpriseBeans.4.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.ejb:jakarta.ejb-api:4.0.1"
kind=ga
edition=core
WLP-Activation-Type: parallel
