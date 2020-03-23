-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.autoSecurityS4U2-1.0
visibility=private
IBM-Provision-Capability: \
 osgi.identity; filter:="(|(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.securityInfrastructure-1.0))(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.ltpa-1.0)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.security.token.s4u2
kind=ga
edition=core
