-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.constrainedDelegation1.0.javaee
visibility = private
IBM-Install-Policy: when-satisfied
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.constrainedDelegation-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.eeCompatible-6.0)(osgi.identity=com.ibm.websphere.appserver.eeCompatible-7.0)(osgi.identity=com.ibm.websphere.appserver.eeCompatible-8.0)))"
-features=\
  com.ibm.websphere.appserver.appSecurity-2.0; ibm.tolerates:="3.0"
kind=ga
edition=core
