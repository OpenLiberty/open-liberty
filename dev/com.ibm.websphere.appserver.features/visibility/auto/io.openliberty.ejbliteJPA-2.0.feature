-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.ejbliteJPA-2.0
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.ejbLiteCore-2.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.jpaContainer-3.0))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.ejbcontainer.jpa.jakarta
kind=beta
edition=core
WLP-Activation-Type: parallel
