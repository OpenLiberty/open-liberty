-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.acme-1.0
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.acme-1.0)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.security.acme
kind=noship
edition=base