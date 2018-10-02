-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.validation-1.1
singleton=true
-features=\
  com.ibm.websphere.appserver.javaeeCompatible-7.0
-bundles=\
  com.ibm.websphere.javaee.validation.1.1; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.validation:validation-api:1.1.0.Final"
kind=ga
edition=core
