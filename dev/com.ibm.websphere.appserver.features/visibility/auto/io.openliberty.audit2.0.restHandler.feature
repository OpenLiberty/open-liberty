-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.audit2.0.restHandler
visibility = private

IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.restHandler.internal-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.audit-2.0))"
IBM-Install-Policy: when-satisfied

-bundles=io.openliberty.request.probe.audit.rest
WLP-Activation-Type: parallel
kind=ga
edition=core
