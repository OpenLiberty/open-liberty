-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.jwtSso1.0.javaee
visibility = private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jwtSso-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.servlet-3.1)(osgi.identity=com.ibm.websphere.appserver.servlet-4.0)))"
IBM-Install-Policy: when-satisfied
-features=\
  com.ibm.websphere.appserver.jsonp-1.0; ibm.tolerates:=1.1
kind=ga
edition=core
