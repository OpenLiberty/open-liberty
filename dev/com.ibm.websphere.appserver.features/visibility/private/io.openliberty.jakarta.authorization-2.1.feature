-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.authorization-2.1
singleton=true
-features=\
  com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=\
  io.openliberty.jakarta.authorization.2.1; location:=dev/api/spec/; mavenCoordinates="jakarta.authorization:jakarta.authorization-api:2.1.0"
kind=ga
edition=core
