-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.springBoot30ServerSupport-1.0
visibility=private
-bundles=com.ibm.ws.springboot.support.web.server.version30

IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.springBoot-3.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-6.0))"
IBM-Install-Policy: when-satisfied

kind=noship
edition=full

