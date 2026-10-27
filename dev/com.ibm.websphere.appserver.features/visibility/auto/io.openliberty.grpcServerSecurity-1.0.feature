-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.grpcServerSecurity-1.0
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.grpc-1.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.appSecurity-1.0)(osgi.identity=com.ibm.websphere.appserver.appSecurity-2.0)(osgi.identity=com.ibm.websphere.appserver.appSecurity-3.0)))"
-bundles=com.ibm.ws.security.authorization.util, \
  io.openliberty.grpc.1.0.internal.server.security
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
WLP-Activation-Type: parallel