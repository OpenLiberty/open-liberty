-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.expressionLanguage-5.0
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=io.openliberty.el.internal.cdi.jakarta, \
 io.openliberty.jakarta.expressionLanguage.5.0; location:="dev/api/spec/,lib/"; mavenCoordinates="org.apache.tomcat:tomcat-el-api:10.1.31"
kind=ga
edition=core
WLP-Activation-Type: parallel
