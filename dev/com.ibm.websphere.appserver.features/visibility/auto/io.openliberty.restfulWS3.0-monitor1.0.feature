-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restfulWS3.0-monitor1.0
Manifest-Version: 1.0
IBM-API-Package: com.ibm.websphere.jaxrs.monitor; type="ibm-api"
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.monitor-1.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.restfulWS-3.0))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.jaxrs.2.x.monitor.jakarta
kind=ga
edition=core
