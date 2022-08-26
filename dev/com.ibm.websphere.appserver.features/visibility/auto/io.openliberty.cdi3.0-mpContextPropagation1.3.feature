-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi3.0-mpContextPropagation1.3
visibility=private
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.cdi-3.0)(osgi.identity=io.openliberty.cdi-4.0)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.mpContextPropagation-1.3))"
-bundles=\
  com.ibm.ws.cdi.mp.context.jakarta
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
