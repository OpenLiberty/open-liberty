-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.pages-3.1
singleton=true
-features=io.openliberty.servlet.api-6.0; apiJar=false, \
  io.openliberty.jakarta.expressionLanguage-5.0; apiJar=false, \
  com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=io.openliberty.jakarta.pages.3.1; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.servlet.jsp:jakarta.servlet.jsp-api:3.1.0"
kind=noship
edition=full
WLP-Activation-Type: parallel
