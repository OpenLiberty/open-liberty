-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.concurrent.mp-1.1
visibility=private
singleton=true
-features=\
  com.ibm.websphere.appserver.contextService-1.0, \
  com.ibm.websphere.appserver.concurrencyPolicy-1.0, \
  com.ibm.websphere.appserver.jakarta.concurrency-2.0, \
  com.ibm.websphere.appserver.org.eclipse.microprofile.contextpropagation-1.1 
-bundles=\
  com.ibm.ws.javaee.platform.defaultresource, \
  com.ibm.websphere.javaee.concurrent.1.0; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.enterprise.concurrent:javax.enterprise.concurrent-api:1.0", \
  com.ibm.ws.resource, \
  com.ibm.ws.concurrent
kind=noship
edition=full
WLP-Activation-Type: parallel
