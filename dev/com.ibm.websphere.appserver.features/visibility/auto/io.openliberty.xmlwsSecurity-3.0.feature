-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.xmlwsSecurity-3.0
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Provision-Capability: \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.xmlWS-3.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.appSecurity-4.0))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.jaxws.2.3.security
kind=beta
edition=base
WLP-Activation-Type: parallel
