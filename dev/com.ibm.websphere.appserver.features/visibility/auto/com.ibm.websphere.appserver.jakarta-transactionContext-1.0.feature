-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jakarta-transactionContext-1.0
visibility=protected
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.contextService-1.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.transaction-2.0)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.transaction.context.jakarta
kind=beta
edition=core
WLP-Activation-Type: parallel
