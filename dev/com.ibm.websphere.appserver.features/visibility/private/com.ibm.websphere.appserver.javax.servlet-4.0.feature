-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.servlet-4.0
singleton=true
-features=\
  com.ibm.websphere.appserver.javaeeCompatible-8.0
-bundles=\
  com.ibm.websphere.javaee.servlet.4.0; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.servlet:javax.servlet-api:4.0.1"
kind=ga
edition=core
