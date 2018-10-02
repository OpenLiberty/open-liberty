-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.ejb-3.1
singleton=true
-features=\
  com.ibm.websphere.appserver.javaeeCompatible-6.0
-bundles=\
  com.ibm.websphere.javaee.ejb.3.1; location:="dev/api/spec/,lib/"; mavenCoordinates="org.glassfish:javax.ejb:3.1"
kind=ga
edition=core
