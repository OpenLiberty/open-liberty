-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.concurrent.mp-1.0
visibility=private
singleton=true
-features=\
com.ibm.websphere.appserver.contextService-1.0
-bundles=\
  com.ibm.websphere.org.eclipse.microprofile.contextpropagation.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.context-propagation:microprofile-context-propagation-api:1.0",\
  com.ibm.ws.concurrent.mp.1.0,\
  com.ibm.ws.javaee.platform.defaultresource, \
  com.ibm.websphere.javaee.concurrent.1.0; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.enterprise.concurrent:javax.enterprise.concurrent-api:1.0", \
  com.ibm.ws.resource, \
  com.ibm.ws.concurrent
kind=ga
edition=core
