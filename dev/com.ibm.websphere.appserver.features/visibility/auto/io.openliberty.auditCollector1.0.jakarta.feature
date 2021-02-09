-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.auditCollector1.0.jakarta
visibility = private

IBM-Provision-Capability:  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.auditCollector-1.0))",\
osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-5.0))"

IBM-Install-Policy: when-satisfied

-bundles=\
  com.ibm.ws.security.audit.source.jakarta

kind=beta
edition=core
