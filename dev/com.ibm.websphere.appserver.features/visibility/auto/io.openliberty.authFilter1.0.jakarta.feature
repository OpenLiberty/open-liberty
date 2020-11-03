-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.authFilter1.0.jakarta
visibility = private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.authFilter-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.servlet.api-5.0))"
IBM-Install-Policy: when-satisfied
-bundles=\
  io.openliberty.security.authentication.internal.filter
kind=noship
edition=full
WLP-Activation-Type: parallel
