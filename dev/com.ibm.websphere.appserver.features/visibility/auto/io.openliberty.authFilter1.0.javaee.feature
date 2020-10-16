-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.authFilter1.0.javaee
visibility = private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.authFilter-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.servlet.api-3.0)(osgi.identity=io.openliberty.servlet.api-3.1)(osgi.identity=io.openliberty.servlet.api-4.0)))"
IBM-Install-Policy: when-satisfied
-bundles=\
  com.ibm.ws.security.authentication.filter
kind=ga
edition=core
WLP-Activation-Type: parallel
