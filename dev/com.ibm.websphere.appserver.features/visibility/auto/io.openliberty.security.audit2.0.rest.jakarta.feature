-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.security.audit2.0.rest.jakarta
visibility = private

IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.restHandler.internal-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.audit-2.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.servlet.internal-5.0)(osgi.identity=io.openliberty.servlet.internal-6.0)(osgi.identity=io.openliberty.servlet.internal-6.1)))"
IBM-Install-Policy: when-satisfied

-bundles=\
  com.ibm.ws.security.audit.rest.jakarta
WLP-Activation-Type: parallel
kind=ga
edition=core
