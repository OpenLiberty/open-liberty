-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.spnego1.0.javaee
visibility = private
IBM-Install-Policy: when-satisfied
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.spnego-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.servlet-3.0)(osgi.identity=com.ibm.websphere.appserver.servlet-3.1)(osgi.identity=com.ibm.websphere.appserver.servlet-4.0)))"
-features=\
  com.ibm.websphere.appserver.appSecurity-2.0; ibm.tolerates:="3.0"
-bundles=\
  com.ibm.ws.security.spnego
kind=ga
edition=core
