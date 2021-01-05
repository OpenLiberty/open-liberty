-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.xmlWSClient.3.0-appSecurity.4.0
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client
IBM-Provision-Capability:  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.xmlWSClient-3.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.appSecurity-4.0))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.jaxws.clientcontainer.security
kind=beta
edition=core
