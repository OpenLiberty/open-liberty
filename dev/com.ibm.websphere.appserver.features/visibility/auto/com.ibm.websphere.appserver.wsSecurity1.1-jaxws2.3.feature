-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.wsSecurity1.1-jaxws2.3
IBM-Provision-Capability:\
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jaxws-2.3))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.wsSecurity-1.1))"
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.wsSecurity1.1.internal.jaxws-2.3, \
  com.ibm.websphere.appserver.httpcommons-1.0
kind=noship
edition=full
