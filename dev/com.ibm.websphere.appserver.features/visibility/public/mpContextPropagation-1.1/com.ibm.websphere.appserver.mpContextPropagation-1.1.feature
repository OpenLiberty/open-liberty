-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpContextPropagation-1.1
visibility=public
singleton=true
IBM-ShortName: mpContextPropagation-1.1
Subsystem-Name: MicroProfile Context Propagation 1.1
IBM-App-ForceRestart: install, uninstall
IBM-API-Package: \
  org.eclipse.microprofile.context; type="stable", \
  org.eclipse.microprofile.context.spi; type="stable"
-features=\
  com.ibm.websphere.appserver.concurrent-2.0; ibm.tolerates:=1.0, \
  com.ibm.websphere.appserver.org.eclipse.microprofile.contextpropagation-1.1
-bundles=\
  com.ibm.ws.microprofile.contextpropagation.1.0
kind=noship
edition=full
