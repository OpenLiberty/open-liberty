-include= ~${workspace}/cnf/resources/bnd/feature.props
 symbolicName=io.openliberty.restfulWSClient3.0-globalhandler2.0
 visibility=private
 IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.restfulWSClient-3.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.globalhandler-2.0))"
 -bundles=io.openliberty.restfulWS.internal.globalhandler
 IBM-Install-Policy: when-satisfied
 kind=beta
 edition=core
 WLP-Activation-Type: parallel