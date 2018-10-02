-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.ejb-3.2
singleton=true
-features=\
  com.ibm.websphere.appserver.javaeeCompatible-7.0; ibm.tolerates:=8.0
-bundles=\
  com.ibm.websphere.javaee.ejb.3.2; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.ejb:javax.ejb-api:3.2"
kind=ga
edition=core
