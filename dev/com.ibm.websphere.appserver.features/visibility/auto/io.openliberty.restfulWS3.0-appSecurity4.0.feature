-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restfulWS3.0-appSecurity4.0
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
Subsystem-Version: 1.1.0
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.restfulWS-3.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.appSecurity-4.0))"
-bundles=io.openliberty.restfulWS30.appSecurity, \
 com.ibm.ws.security.authorization.util.jakarta
IBM-Install-Policy: when-satisfied
kind=beta
edition=core
WLP-Activation-Type: parallel
