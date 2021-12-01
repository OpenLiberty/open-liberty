-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.authentication-3.0
singleton=true
-features=\
  com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=\
  io.openliberty.jakarta.authentication.2.0; location:=dev/api/spec/; mavenCoordinates="jakarta.authentication:jakarta.authentication-api:2.0.0"
kind=noship
edition=full

