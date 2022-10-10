-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restfulWS3.1-globalHandler1.0
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.restfulWS-3.1)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.globalhandler-1.0)))"
-features=\
  io.openliberty.globalhandler1.0.internal.ee-10.0
-bundles=\
  io.openliberty.restfulWS.internal.globalhandler
IBM-Install-Policy: when-satisfied
kind=beta
edition=core
WLP-Activation-Type: parallel
