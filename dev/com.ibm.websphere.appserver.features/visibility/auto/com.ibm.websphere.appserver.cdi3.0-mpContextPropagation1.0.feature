-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.cdi3.0-mpContextPropagation1.0
visibility=private
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.cdi-3.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.mpContextPropagation-1.0)(osgi.identity=com.ibm.websphere.appserver.mpContextPropagation-1.1)))"
-bundles=\
  com.ibm.ws.cdi.mp.context.jakarta
IBM-Install-Policy: when-satisfied
kind=noship
edition=core
