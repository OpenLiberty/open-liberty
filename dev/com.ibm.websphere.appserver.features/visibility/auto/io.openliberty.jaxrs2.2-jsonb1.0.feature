-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jaxrs2.2-jsonb1.0
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
Subsystem-Version: 1.1.0
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jaxrsClient-2.2))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jsonb-1.0))"
-bundles=io.openliberty.restfulWS30.jsonb20provider
IBM-Install-Policy: when-satisfied
kind=noship
edition=base
WLP-Activation-Type: parallel
