-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restfulWSDefaultExceptionMapper-1.0
Manifest-Version: 1.0
visibility=private
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.mpMetrics-4.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.monitor-1.0)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.restfulWS-3.0)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.jaxrs.defaultexceptionmapper.jakarta
kind=ga
edition=core