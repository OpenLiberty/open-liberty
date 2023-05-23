-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.springBootServerSupport.servlet60-1.0
visibility=private
-bundles=com.ibm.ws.springboot.support.web.server.jakarta

IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.springBootHandler-3.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-6.0))"
IBM-Install-Policy: when-satisfied
IBM-API-Package: com.ibm.ws.springboot.support.web.server.initializer; type="internal"

kind=noship
edition=full

