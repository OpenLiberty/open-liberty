-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsonb-cdi-1.0
visibility=private
IBM-Install-Policy: when-satisfied
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.cdi-2.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jsonbInternal-1.0))"
-bundles=com.ibm.ws.jsonb.cdi
kind=beta
edition=core
