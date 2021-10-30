-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.csiv2-1.0
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.ejbRemote-3.2))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.appSecurity-2.0)(osgi.identity=com.ibm.websphere.appserver.appSecurity-3.0)))"
IBM-Install-Policy: when-satisfied
-bundles=\
  com.ibm.ws.security.csiv2.common, \
  com.ibm.ws.security.csiv2
kind=ga
edition=base
