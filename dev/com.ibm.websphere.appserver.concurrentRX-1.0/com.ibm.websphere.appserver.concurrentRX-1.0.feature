-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.concurrentRX-1.0
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.concurrent-1.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jaxrs-2.1))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.concurrent.rx
kind=beta
edition=core
