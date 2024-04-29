-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jaxws-concurrent
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.concurrent-3.1)(osgi.identity=io.openliberty.concurrent-3.0)(osgi.identity=io.openliberty.concurrent-2.0)(osgi.identity=com.ibm.websphere.appserver.concurrent-1.0)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.xmlWS-4.0)(osgi.identity=io.openliberty.xmlWS-3.0)(osgi.identity=com.ibm.websphere.appserver.jaxws-2.2)))"
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.contextService-1.0
-bundles=io.openliberty.cxf.concurrent
kind=ga
edition=base
