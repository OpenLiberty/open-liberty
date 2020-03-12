-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jakarta.jsp-3.0
singleton=true
-features=com.ibm.websphere.appserver.jakarta.el-4.0; apiJar=false, \
 com.ibm.websphere.appserver.jakarta.servlet-5.0; apiJar=false
-bundles=com.ibm.websphere.jakartaee.jsp.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.servlet.jsp:jakarta.servlet.jsp-api:3.0.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
