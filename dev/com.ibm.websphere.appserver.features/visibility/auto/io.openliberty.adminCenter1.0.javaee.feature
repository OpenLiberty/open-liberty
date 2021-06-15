-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.adminCenter1.0.javaee
visibility = private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.adminCenter-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.servlet-3.0)(osgi.identity=com.ibm.websphere.appserver.servlet-3.1)(osgi.identity=com.ibm.websphere.appserver.servlet-4.0)))"
IBM-Install-Policy: when-satisfied
-features=\
  com.ibm.websphere.appserver.restConnector-1.0; ibm.tolerates:="2.0", \
  com.ibm.websphere.appserver.jta-1.1; ibm.tolerates:="1.2", \
  com.ibm.websphere.appserver.jsp-2.2; ibm.tolerates:="2.3", \
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0"
kind=ga
edition=core
