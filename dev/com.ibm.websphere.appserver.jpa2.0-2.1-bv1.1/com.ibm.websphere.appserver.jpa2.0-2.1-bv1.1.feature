-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jpa2.0-2.1-bv1.1
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.jpa-2.0)(osgi.identity=com.ibm.websphere.appserver.jpaContainer-2.1)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.beanValidation-1.1)(osgi.identity=com.ibm.websphere.appserver.beanValidation-2.0)))"
-bundles=com.ibm.ws.jpa.container.beanvalidation.1.1
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
