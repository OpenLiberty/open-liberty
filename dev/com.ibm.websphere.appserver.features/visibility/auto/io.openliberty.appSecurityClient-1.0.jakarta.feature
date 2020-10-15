-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.appSecurityClient-1.0.jakarta
visibility = private
IBM-Install-Policy: when-satisfied
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.appSecurityClient-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.servlet.api-5.0))"
-bundles=\
  io.openliberty.security.jaas.internal.common
kind=noship
edition=full
