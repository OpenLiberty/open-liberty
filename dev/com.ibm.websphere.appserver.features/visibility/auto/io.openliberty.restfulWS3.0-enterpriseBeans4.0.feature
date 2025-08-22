-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restfulWS3.0-enterpriseBeans4.0
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
Subsystem-Version: 1.1.0
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.restfulWS-3.0)(osgi.identity=io.openliberty.restfulWS-3.1)(osgi.identity=io.openliberty.restfulWS-4.0)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.enterpriseBeans-4.0)))"
-bundles=io.openliberty.restfulWS.internal.ejb
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
WLP-Activation-Type: parallel