-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.springBoot40ServerSupport.servlet61-1.0
visibility=private
-bundles=io.openliberty.springboot.support.web.server.version40, \
 com.ibm.ws.springboot.support.web.server.jakarta

IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.springBoot-4.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-6.1))"
IBM-Install-Policy: when-satisfied
IBM-API-Package: com.ibm.ws.springboot.support.web.server.initializer; type="internal"

kind=noship
edition=full

