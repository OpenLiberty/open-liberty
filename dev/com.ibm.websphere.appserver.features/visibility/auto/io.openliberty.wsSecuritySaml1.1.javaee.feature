-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.wsSecuritySaml1.1.javaee
visibility = private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.wsSecuritySaml-1.1))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.servlet-3.0)(osgi.identity=com.ibm.websphere.appserver.servlet.api-3.1)(osgi.identity=com.ibm.websphere.appserver.servlet.api-4.0)))"
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.jta-1.1; ibm.tolerates:="1.2"
kind=ga
edition=base
