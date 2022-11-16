-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.servlet.api-6.0
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=io.openliberty.jakarta.servlet.6.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.servlet:jakarta.servlet-api:6.0.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
