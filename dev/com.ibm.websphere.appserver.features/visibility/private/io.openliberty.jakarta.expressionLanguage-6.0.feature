-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.expressionLanguage-6.0
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-11.0, \
  io.openliberty.noShip-1.0
-bundles=io.openliberty.el.internal.cdi.jakarta, \
 io.openliberty.jakarta.expressionLanguage.6.0; location:="dev/api/spec/,lib/"; mavenCoordinates="org.apache.tomcat:tomcat-el-api:11.0.0"
kind=noship
edition=full
WLP-Activation-Type: parallel
