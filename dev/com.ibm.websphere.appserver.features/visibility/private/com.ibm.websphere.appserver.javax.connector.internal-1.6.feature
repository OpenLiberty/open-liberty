-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.connector.internal-1.6
singleton=true
-features=\
  com.ibm.websphere.appserver.javaeeCompatible-6.0
-bundles=\
  com.ibm.websphere.javaee.connector.1.6; apiJar=false; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.resource:javax.resource-api:1.7"
kind=ga
edition=core
