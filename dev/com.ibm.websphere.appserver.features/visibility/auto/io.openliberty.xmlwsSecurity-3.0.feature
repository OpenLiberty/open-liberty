-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.xmlwsSecurity-3.0
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Provision-Capability: \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.xmlWS-3.0)(osgi.identity=io.openliberty.xmlWS-4.0)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.appSecurity-4.0)(osgi.identity=io.openliberty.appSecurity-5.0)(osgi.identity=io.openliberty.appSecurity-6.0)(osgi.identity=io.openliberty.mpJwt-2.1)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.jaxws.security
kind=ga
edition=base
WLP-Activation-Type: parallel
