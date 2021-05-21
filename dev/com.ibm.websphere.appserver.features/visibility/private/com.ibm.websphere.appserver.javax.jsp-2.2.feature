-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.jsp-2.2
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-features=io.openliberty.servlet.api-3.0; apiJar=false; ibm.tolerates:="3.1", \
  com.ibm.websphere.appserver.eeCompatible-6.0; ibm.tolerates:="7.0,8.0", \
  com.ibm.websphere.appserver.javax.el-2.2; apiJar=false
-bundles=com.ibm.websphere.javaee.jsp.2.2; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.servlet.jsp:javax.servlet.jsp-api:2.2.1"
kind=ga
edition=core
