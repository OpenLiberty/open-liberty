-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.securityContext-1.0
visibility=protected
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.contextService-1.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.securityInfrastructure-1.0))"
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.securityInfrastructure-1.0
-bundles=com.ibm.ws.security.context
kind=ga
edition=core
WLP-Activation-Type: parallel
