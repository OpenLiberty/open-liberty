-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.ejbliteJPA-1.0
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.ejbLiteCore-1.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.jpa-2.0)(osgi.identity=com.ibm.websphere.appserver.jpaContainer-2.1)(osgi.identity=com.ibm.websphere.appserver.jpaContainer-2.2)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.ejbcontainer.jpa
kind=ga
edition=core
WLP-Activation-Type: parallel
