-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.appSecurityClient-1.0.javaee
visibility = private
IBM-Install-Policy: when-satisfied
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.appSecurityClient-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.servlet.api-3.0)(osgi.identity=io.openliberty.servlet.api-3.1)(osgi.identity=io.openliberty.servlet.api-4.0)))"
-bundles=\
  com.ibm.ws.security.jaas.common
kind=ga
edition=base
