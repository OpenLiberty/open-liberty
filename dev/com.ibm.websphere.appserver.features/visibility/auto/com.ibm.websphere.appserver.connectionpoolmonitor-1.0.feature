-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.connectionpoolmonitor-1.0
IBM-API-Package: com.ibm.websphere.connectionpool.monitor; type="ibm-api"
Manifest-Version: 1.0
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.monitor-1.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.connectionManagement-1.0))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.connectionpool.monitor
-jars=com.ibm.websphere.appserver.api.connectionpool; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.connectionpool_1.1-javadoc.zip
kind=ga
edition=core
