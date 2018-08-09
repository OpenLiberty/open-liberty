-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpJwtExtension-1.1
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.mpConfig-1.1)(osgi.identity=com.ibm.websphere.appserver.mpConfig-1.2)(osgi.identity=com.ibm.websphere.appserver.mpConfig-1.3)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpJwt-1.1))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.security.mp.jwt.1.1
kind=noship
edition=full
