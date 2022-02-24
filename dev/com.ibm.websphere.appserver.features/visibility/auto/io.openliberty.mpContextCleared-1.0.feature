-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpContextCleared-1.0
visibility=private
# Do not add 1.4+ or 2.0+ features. That context must not be cleared in order to
# avoid interference with third-party context from Jakarta EE 10/Concurrency 3.0+
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.mpContextPropagation-1.3)(osgi.identity=com.ibm.websphere.appserver.mpContextPropagation-1.2)(osgi.identity=com.ibm.websphere.appserver.mpContextPropagation-1.0)))"
-bundles=\
  io.openliberty.microprofile.context.cleared
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
