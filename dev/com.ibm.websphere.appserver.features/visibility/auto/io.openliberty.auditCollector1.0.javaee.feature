-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.auditCollector1.0.javaee
visibility = private

IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.auditCollector-1.0))",\
osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.servlet-3.0)(osgi.identity=com.ibm.websphere.appserver.servlet-3.1)(osgi.identity=com.ibm.websphere.appserver.servlet-4.0)))"

IBM-Install-Policy: when-satisfied

-bundles=\
  com.ibm.ws.security.audit.source

kind=ga
edition=core
