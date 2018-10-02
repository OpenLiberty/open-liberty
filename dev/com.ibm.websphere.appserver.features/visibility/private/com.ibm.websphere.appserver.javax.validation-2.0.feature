-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.validation-2.0
singleton=true
-features=\
  com.ibm.websphere.appserver.javaeeCompatible-8.0
-bundles=\
  com.ibm.websphere.javaee.validation.2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.validation:validation-api:2.0.1.Final"
kind=ga
edition=core
