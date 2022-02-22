-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpContextCleared-concurrent
visibility=private
# Do not add newer feature versions to this. For newer versions, context must not be cleared
# in order to avoid interference with third-party context from Jakarta EE 10/Concurrency 3.0+
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.concurrent-2.0)(osgi.identity=com.ibm.websphere.appserver.concurrent-1.0)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.mpContextPropagation-1.3)(osgi.identity=com.ibm.websphere.appserver.mpContextPropagation-1.2)(osgi.identity=com.ibm.websphere.appserver.mpContextPropagation-1.0)))"
-bundles=\
  io.openliberty.microprofile.context.cleared.internal
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
