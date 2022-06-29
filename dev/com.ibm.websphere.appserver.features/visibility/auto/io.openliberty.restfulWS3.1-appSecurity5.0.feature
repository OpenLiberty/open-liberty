-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restfulWS3.1-appSecurity5.0
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
Subsystem-Version: 1.1.0
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.restfulWS-3.1))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.appSecurity-5.0))"
-bundles=io.openliberty.restfulWS31.appSecurity, \
 com.ibm.ws.security.authorization.util.jakarta
IBM-Install-Policy: when-satisfied
kind=noship
edition=full
WLP-Activation-Type: parallel
