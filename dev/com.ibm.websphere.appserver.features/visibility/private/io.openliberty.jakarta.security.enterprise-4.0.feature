-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.security.enterprise-4.0
singleton=true
-features=\
  com.ibm.websphere.appserver.eeCompatible-11.0, \
  io.openliberty.noShip-1.0
-bundles=\
  io.openliberty.jakarta.security.3.0; location:=dev/api/spec/; mavenCoordinates="jakarta.security.enterprise:jakarta.security.enterprise-api:3.0.0"
kind=noship
edition=full

