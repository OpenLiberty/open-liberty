-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.cdi2.0-mpConcurrency1.0
visibility=private
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.cdi-2.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpConcurrency-1.0))"
-bundles=com.ibm.ws.concurrent.mp.cdi
IBM-Install-Policy: when-satisfied
kind=noship
edition=full
