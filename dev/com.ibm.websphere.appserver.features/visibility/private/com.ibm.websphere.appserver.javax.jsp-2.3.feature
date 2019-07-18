-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.jsp-2.3
singleton=true
-features=com.ibm.websphere.appserver.javax.el-3.0; apiJar=false, \
 com.ibm.websphere.appserver.javax.servlet-3.1; ibm.tolerates:=4.0; apiJar=false
-bundles=com.ibm.websphere.javaee.jsp.2.3; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.servlet.jsp:javax.servlet.jsp-api:2.3.1"
kind=ga
edition=core
WLP-Activation-Type: parallel
