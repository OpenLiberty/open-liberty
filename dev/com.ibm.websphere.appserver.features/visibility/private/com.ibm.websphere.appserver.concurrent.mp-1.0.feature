-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.concurrent.mp-1.0
visibility=private
singleton=true
-bundles=\
<<<<<<< HEAD
  com.ibm.websphere.org.eclipse.microprofile.contextpropagation.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="com.ibm.ws.org.eclipse.microprofile.context-propagation:microprofile-context-propagation-api:1.0",\
  com.ibm.ws.concurrent.mp.1.0
=======
  com.ibm.websphere.org.eclipse.microprofile.contextpropagation.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="com.ibm.ws.org.eclipse.microprofile.context-propagation:microprofile-context-propagation-api:1.0-RC3",\
  com.ibm.ws.concurrent.mp.1.0,\
  com.ibm.ws.javaee.platform.defaultresource, \
  com.ibm.websphere.javaee.concurrent.1.0; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.enterprise.concurrent:javax.enterprise.concurrent-api:1.0", \
  com.ibm.ws.resource, \
  com.ibm.ws.concurrent  
>>>>>>> Issue #7394: Use MP Context Propagation for JAX-RS Reactive Client
kind=ga
edition=core
