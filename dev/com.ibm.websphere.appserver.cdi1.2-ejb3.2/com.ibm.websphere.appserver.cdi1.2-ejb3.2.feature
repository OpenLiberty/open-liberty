-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.cdi1.2-ejb3.2
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.cdi-1.2))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.ejbCore-1.0))"
-bundles=com.ibm.ws.cdi.1.2.ejb
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
