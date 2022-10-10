-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.xmlWS4.0-globalHandler1.0
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.restfulWS-3.1))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.globalhandler-1.0))"
-bundles=\
  io.openliberty.jaxws.globalhandler.internal.jakarta
IBM-Install-Policy: when-satisfied
kind=beta
edition=base
WLP-Activation-Type: parallel
