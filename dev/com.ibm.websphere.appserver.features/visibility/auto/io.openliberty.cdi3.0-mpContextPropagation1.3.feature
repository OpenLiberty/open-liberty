-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi3.0-mpContextPropagation1.3
visibility=private
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.cdi-3.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.mpContextPropagation-1.3)(osgi.identity=com.ibm.websphere.appserver.mpContextPropagation-1.2)))"
-bundles=\
  com.ibm.ws.cdi.mp.context.jakarta
IBM-Install-Policy: when-satisfied
kind=noship
edition=full
