-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.servlet.api-5.0
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=io.openliberty.jakarta.servlet.5.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.servlet:jakarta.servlet-api:5.0.0"
kind=beta
edition=core
WLP-Activation-Type: parallel
