-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.cdi-4.1
singleton=true
-features=io.openliberty.jakarta.expressionLanguage-6.0; apiJar=false, \
  com.ibm.websphere.appserver.eeCompatible-11.0, \
  io.openliberty.jakarta.interceptor-2.2, \
  io.openliberty.noShip-1.0
-bundles=io.openliberty.jakarta.cdi.4.1; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.enterprise:jakarta.enterprise.cdi-api:4.1.0.Alpha1"
kind=noship
edition=full
WLP-Activation-Type: parallel
