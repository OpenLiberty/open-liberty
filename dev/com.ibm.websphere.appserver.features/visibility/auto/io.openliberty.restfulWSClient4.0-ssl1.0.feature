-include= ~${workspace}/cnf/resources/bnd/feature.props
 symbolicName=io.openliberty.restfulWSClient4.0-ssl1.0
 visibility=private
 IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.restfulWSClient-4.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.ssl-1.0))"
 -bundles=io.openliberty.restfulWS.internal.ssl.ee11
 IBM-Install-Policy: when-satisfied
 kind=beta
 edition=core