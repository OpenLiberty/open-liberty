-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.pages-3.0
singleton=true
-features=io.openliberty.servlet.api-5.0; apiJar=false, \
  io.openliberty.jakarta.expressionLanguage-4.0; apiJar=false, \
  com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=io.openliberty.jakarta.pages.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.servlet.jsp:jakarta.servlet.jsp-api:3.0.0"
kind=beta
edition=core
WLP-Activation-Type: parallel
