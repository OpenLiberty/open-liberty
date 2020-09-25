-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.audit1.0.javaee
visibility = private

IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.audit-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.servlet-3.0)(osgi.identity=com.ibm.websphere.appserver.servlet-3.1)(osgi.identity=com.ibm.websphere.appserver.servlet-4.0)))"

IBM-Install-Policy: when-satisfied

-features=\
  com.ibm.websphere.appserver.appSecurity-2.0; ibm.tolerates:="3.0"

-bundles=\
  com.ibm.ws.security.audit.file, \
  com.ibm.ws.request.probe.audit.servlet

kind=ga
edition=core
