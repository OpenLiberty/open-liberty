-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.cdi-1.2
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-7.0; ibm.tolerates:="6.0,8.0", \
  com.ibm.websphere.appserver.javax.el-3.0; apiJar=false, \
  com.ibm.websphere.appserver.javax.interceptor-1.2
-bundles=com.ibm.websphere.javaee.cdi.1.2; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.enterprise:cdi-api:1.2"
kind=ga
edition=core
WLP-Activation-Type: parallel
