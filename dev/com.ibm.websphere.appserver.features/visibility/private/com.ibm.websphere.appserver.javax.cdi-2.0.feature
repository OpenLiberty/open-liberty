-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.cdi-2.0
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-8.0; ibm.tolerates:="6.0,7.0", \
  com.ibm.websphere.appserver.javax.el-3.0; apiJar=false, \
  com.ibm.websphere.appserver.javax.interceptor-1.2
-bundles=com.ibm.websphere.javaee.cdi.2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.enterprise:cdi-api:2.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
