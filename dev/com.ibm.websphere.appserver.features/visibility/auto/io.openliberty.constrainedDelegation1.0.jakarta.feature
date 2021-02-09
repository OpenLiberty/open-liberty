-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.constrainedDelegation1.0.jakarta
visibility = private
IBM-Install-Policy: when-satisfied
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.constrainedDelegation-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.eeCompatible-9.0))"
-features=\
  io.openliberty.appSecurity-4.0
kind=beta
edition=core
