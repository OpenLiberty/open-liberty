-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.distributedMapPMI-1.0
Manifest-Version: 1.0
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.monitor-1.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.distributedMap-1.0))"
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.distributedMap-1.0, \
  com.ibm.websphere.appserver.monitor-1.0
-bundles=com.ibm.ws.dynacache.monitor
kind=ga
edition=core
