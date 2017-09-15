-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javaeePlatform7.0-jndi1.0
visibility=private
IBM-Process-Types: client, \
 server
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.javaeePlatform-7.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jndi-1.0))"
-bundles=com.ibm.ws.javaee.platform.v7.jndi
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
