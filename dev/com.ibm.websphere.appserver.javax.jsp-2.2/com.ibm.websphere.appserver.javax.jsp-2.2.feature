-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.jsp-2.2
singleton=true
-features=com.ibm.websphere.appserver.javax.servlet-3.0; ibm.tolerates:=3.1; apiJar=false, \
 com.ibm.websphere.appserver.javax.el-2.2; apiJar=false
-bundles=com.ibm.websphere.javaee.jsp.2.2; location:="dev/api/spec/,lib/"
kind=ga
edition=core
