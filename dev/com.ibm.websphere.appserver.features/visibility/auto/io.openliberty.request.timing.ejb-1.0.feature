-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.request.timing.ejb-1.0
Manifest-Version: 1.0
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.noShip-1.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.requestTiming-1.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.ejbCore-1.0)(osgi.identity=io.openliberty.ejbCore-2.0)))"
IBM-Install-Policy: when-satisfied
-bundles=io.openliberty.request.timing.ejb.internal
kind=noship
edition=full
