-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jakarta.cdi-3.0
singleton=true
-features=io.openliberty.appserver.jakarta.el-4.0; apiJar=false, \
 com.ibm.websphere.appserver.jakarta.interceptor-2.0
-bundles=com.ibm.websphere.jakarta.cdi.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.enterprise:jakarta.enterprise.cdi-api:3.0.0"
kind=noship
edition=core
WLP-Activation-Type: parallel
