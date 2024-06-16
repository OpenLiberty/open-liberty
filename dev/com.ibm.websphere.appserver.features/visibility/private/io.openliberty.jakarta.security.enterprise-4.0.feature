-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.security.enterprise-4.0
singleton=true
-features=\
  com.ibm.websphere.appserver.eeCompatible-11.0
-bundles=\
  io.openliberty.jakarta.security.4.0; location:=dev/api/spec/; mavenCoordinates="jakarta.security.enterprise:jakarta.security.enterprise-api:4.0.0-M1"
 kind=ga
edition=core

