-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.classloaderContext-1.0
visibility=protected
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.contextService-1.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.classloading-1.0))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.classloader.context
kind=ga
edition=core
