-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.emptyHandleListContext-1.0
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.contextService-1.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.connectionManagement-1.0))"
IBM-Install-Policy: when-satisfied
-bundles=io.openliberty.handlelist.context.internal
kind=ga
edition=core
WLP-Activation-Type: parallel
