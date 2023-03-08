-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.authentication-3.0
singleton=true
-features=\
  com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=\
  io.openliberty.jakarta.authentication.3.0; location:=dev/api/spec/; mavenCoordinates="jakarta.authentication:jakarta.authentication-api:3.0.0"
kind=ga
edition=core

