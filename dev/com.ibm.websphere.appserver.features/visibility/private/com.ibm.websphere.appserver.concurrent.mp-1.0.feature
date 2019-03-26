-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.concurrent.mp-1.0
visibility=private
singleton=true
-bundles=\
  com.ibm.websphere.org.eclipse.microprofile.contextpropagation.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="com.ibm.ws.org.eclipse.microprofile.context-propagation:microprofile-context-propagation-api:0.20190322.204445",\
  com.ibm.ws.concurrent.mp.1.0
kind=beta
edition=core
