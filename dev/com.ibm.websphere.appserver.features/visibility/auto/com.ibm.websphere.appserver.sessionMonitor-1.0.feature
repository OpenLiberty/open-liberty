-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.sessionMonitor-1.0
IBM-API-Package: com.ibm.websphere.session.monitor; type="ibm-api"
Manifest-Version: 1.0
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.monitor-1.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.servlet-3.0)(osgi.identity=com.ibm.websphere.appserver.servlet-3.1)(osgi.identity=com.ibm.websphere.appserver.servlet-4.0)(osgi.identity=com.ibm.websphere.appserver.servlet-5.0)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.session.monitor
-jars=com.ibm.websphere.appserver.api.sessionstats; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.sessionstats_1.0-javadoc.zip
kind=ga
edition=core
