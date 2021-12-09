-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.autoTimingMonitor-1.0
IBM-App-ForceRestart: uninstall
Manifest-Version: 1.0
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.monitor-1.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.requestTiming-1.0))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.request.timing.monitor
IBM-API-Package: com.ibm.websphere.request.timing; type="ibm-api"
-jars=com.ibm.websphere.appserver.api.requestTimingMonitor; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.requestTimingMonitor_1.0-javadoc.zip
kind=ga
edition=core
