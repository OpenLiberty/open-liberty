-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.openidConnectServer1.0.javaee
visibility = private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.openidConnectServer-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.servlet-3.0)(osgi.identity=com.ibm.websphere.appserver.servlet-3.1)(osgi.identity=com.ibm.websphere.appserver.servlet-4.0)))"
IBM-Install-Policy: when-satisfied
-features=\
  com.ibm.websphere.appserver.jsp-2.2; ibm.tolerates:=2.3, \
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0"
kind=ga
edition=core
