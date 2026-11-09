-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restJwt-1.0
visibility=private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jwt-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.restConnector-2.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.servlet-6.0)(osgi.identity=com.ibm.websphere.appserver.servlet-6.1)))"
IBM-Install-Policy: when-satisfied
-features=\
  io.openliberty.restJwt1.0.internal.ee-10.0; ibm.tolerates:="11.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
