-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.wsSecuritySaml1.1-jaxws2.2
IBM-Provision-Capability:\
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.jaxws-2.2)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.wsSecuritySaml-1.1))"
IBM-Install-Policy: when-satisfied
-features=\
  com.ibm.websphere.appserver.jta-1.1; ibm.tolerates:=1.2, \
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0", \
  com.ibm.websphere.appserver.samlWeb-2.0
kind=ga
edition=base