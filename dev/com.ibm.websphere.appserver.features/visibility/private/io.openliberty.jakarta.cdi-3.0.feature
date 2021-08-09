-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.cdi-3.0
singleton=true
-features=io.openliberty.jakarta.expressionLanguage-4.0; apiJar=false, \
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  io.openliberty.jakarta.interceptor-2.0
-bundles=io.openliberty.jakarta.cdi.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.enterprise:jakarta.enterprise.cdi-api:3.0.0"
kind=beta
edition=core
WLP-Activation-Type: parallel
