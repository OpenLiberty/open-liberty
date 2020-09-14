-include= ~${workspace}/cnf/resources/bnd/feature.props
 symbolicName=io.openliberty.jaxrs2.2-ssl1.0
 visibility=private
 IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.jaxrs-2.2))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.ssl-1.0))"
 -bundles=io.openliberty.restfulWS.internal.ssl
 IBM-Install-Policy: when-satisfied
 kind=noship
 edition=base