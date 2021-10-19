-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxrsDefaultExceptionMapper-1.0
Manifest-Version: 1.0
visibility=private
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpMetrics-3.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.monitor-1.0)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.jaxrs-2.0)(osgi.identity=com.ibm.websphere.appserver.jaxrs-2.1)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.jaxrs.defaultexceptionmapper, \
         com.ibm.ws.jaxrs.2.x.defaultexceptionmapper
kind=ga
edition=core