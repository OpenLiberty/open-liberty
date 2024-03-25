-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.authorization-3.0
singleton=true
-features=\
  com.ibm.websphere.appserver.eeCompatible-11.0, \
  io.openliberty.noShip-1.0
-bundles=\
  io.openliberty.jakarta.authorization.3.0; location:=dev/api/spec/; mavenCoordinates="jakarta.authorization:jakarta.authorization-api:3.0.0-M1"
kind=noship
edition=full
