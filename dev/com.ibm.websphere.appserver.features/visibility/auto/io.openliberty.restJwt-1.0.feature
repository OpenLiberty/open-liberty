-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restJwt-1.0
visibility=private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jwt-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.restConnector-2.0))"
IBM-Install-Policy: when-satisfied
-bundles=\
  io.openliberty.security.rest.jwt
kind=ga
edition=core
WLP-Activation-Type: parallel
