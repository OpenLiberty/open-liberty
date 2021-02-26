-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.spnego1.0.jakarta
visibility = private
IBM-Install-Policy: when-satisfied
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.spnego-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-5.0))"
-features=\
  io.openliberty.appSecurity-4.0
-bundles=\
  io.openliberty.security.spnego.internal
kind=beta
edition=core
