-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.audit1.0.jakarta
visibility = private

IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.audit-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-5.0))"

IBM-Install-Policy: when-satisfied

-features=\
  io.openliberty.appSecurity-4.0
  
-bundles=\
  com.ibm.ws.security.audit.file.jakarta,\
  com.ibm.ws.request.probe.audit.servlet.jakarta

kind=beta
edition=core
