-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.xmlwsEjb-3.0
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Provision-Capability: \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.xmlWS-3.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.ejbLiteCore-2.0))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.jaxws.2.3.ejb.jakarta
kind=beta
edition=base
WLP-Activation-Type: parallel
