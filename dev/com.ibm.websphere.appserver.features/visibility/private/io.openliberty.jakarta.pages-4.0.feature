-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.pages-4.0
singleton=true
-features=io.openliberty.servlet.api-6.1; apiJar=false, \
  io.openliberty.jakarta.expressionLanguage-6.0; apiJar=false, \
  com.ibm.websphere.appserver.eeCompatible-11.0, \
  io.openliberty.noShip-1.0
-bundles=io.openliberty.jakarta.pages.4.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.servlet.jsp:jakarta.servlet.jsp-api:4.0.0"
kind=noship
edition=full
WLP-Activation-Type: parallel
