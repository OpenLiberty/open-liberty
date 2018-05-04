-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.jsp-2.2
singleton=true
-features=com.ibm.websphere.appserver.javax.servlet-3.0; ibm.tolerates:=3.1; apiJar=false, \
 com.ibm.websphere.appserver.javax.el-2.2; apiJar=false
-bundles=com.ibm.websphere.javaee.jsp.2.2; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.servlet.jsp:javax.servlet.jsp-api:2.2.1"
kind=ga
edition=core
