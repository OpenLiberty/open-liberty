-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.cdi-4.0
singleton=true
-features=io.openliberty.jakarta.expressionLanguage-5.0; apiJar=false, \
  com.ibm.websphere.appserver.eeCompatible-10.0, \
  io.openliberty.jakarta.interceptor-2.0
-bundles=io.openliberty.jakarta.cdi.4.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.enterprise:jakarta.enterprise.cdi-api:4.0.0"
kind=noship
edition=full
WLP-Activation-Type: parallel
