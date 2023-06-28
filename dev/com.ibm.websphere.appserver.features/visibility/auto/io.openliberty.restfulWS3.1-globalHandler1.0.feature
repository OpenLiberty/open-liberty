-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restfulWS3.1-globalHandler1.0
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.restfulWSClient-3.1)(osgi.identity=io.openliberty.restfulWSClient-4.0)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.globalhandler-1.0))"
-bundles=\
  io.openliberty.restfulWS.internal.globalhandler
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
WLP-Activation-Type: parallel
