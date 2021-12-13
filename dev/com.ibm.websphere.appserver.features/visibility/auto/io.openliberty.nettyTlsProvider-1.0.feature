-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.nettyTlsProvider-1.0
Manifest-Version: 1.0
visibility=private
IBM-App-ForceRestart: install, uninstall
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.netty-1.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.ssl-1.0))"
IBM-Install-Policy: when-satisfied
-bundles=\
  io.openliberty.netty.internal.tls.impl
kind=noship
edition=core
WLP-Activation-Type: parallel
