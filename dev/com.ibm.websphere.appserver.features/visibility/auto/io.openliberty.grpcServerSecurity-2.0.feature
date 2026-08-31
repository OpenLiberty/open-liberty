-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.grpcServerSecurity-2.0
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.grpc-1.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.appSecurity-4.0)(osgi.identity=io.openliberty.appSecurity-5.0)(osgi.identity=io.openliberty.appSecurity-6.0)(osgi.identity=io.openliberty.appSecurity-7.0)(osgi.identity=io.openliberty.mpJwt-2.1)))"
-bundles=com.ibm.ws.security.authorization.util.jakarta, \
  io.openliberty.grpc.1.0.internal.server.security.jakarta
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
WLP-Activation-Type: parallel