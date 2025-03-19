-include= ~${workspace}/cnf/resources/bnd/feature.props
 symbolicName=io.openliberty.restfulWSClient3.0-ssl1.0
 visibility=private
 IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.restfulWSClient-3.0)(osgi.identity=io.openliberty.restfulWSClient-3.1)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.ssl-1.0))"
 -bundles=io.openliberty.restfulWS.internal.ssl
 IBM-Install-Policy: when-satisfied
 kind=ga
 edition=core