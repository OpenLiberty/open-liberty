-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.transportSecurity1.0.jakarta
visibility = private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.ssl-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.eeCompatible-9.0)(osgi.identity=com.ibm.websphere.appserver.eeCompatible-10.0)))"
IBM-Install-Policy: when-satisfied
-features=\
  com.ibm.websphere.appserver.transportSecurity-1.0
kind=ga
edition=core
