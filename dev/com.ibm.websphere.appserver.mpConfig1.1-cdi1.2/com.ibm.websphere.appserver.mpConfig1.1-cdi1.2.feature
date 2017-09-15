-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpConfig1.1-cdi1.2
visibility=private
singleton=true
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpConfig-1.1))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.cdi-1.2))"
-bundles=com.ibm.ws.microprofile.config.cdi
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
