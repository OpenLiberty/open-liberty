-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.auditWebProbes-1.0
visibility=private
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.servlet-3.0)(osgi.identity=com.ibm.websphere.appserver.servlet-3.1)(osgi.identity=com.ibm.websphere.appserver.servlet-4.0)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.appSecurity-1.0)(osgi.identity=com.ibm.websphere.appserver.appSecurity-2.0)(osgi.identity=com.ibm.websphere.appserver.appSecurity-3.0)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.audit-1.0))"
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.requestProbes-1.0
-bundles=com.ibm.ws.request.probe.audit.servlet, \
com.ibm.ws.security.audit.source
kind=ga
edition=core
