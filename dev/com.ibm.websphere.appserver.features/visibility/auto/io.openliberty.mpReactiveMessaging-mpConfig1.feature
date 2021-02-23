-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpReactiveMessaging-mpConfig1
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpReactiveMessaging-1.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.mpConfig-1.3) \
                                                          (osgi.identity=com.ibm.websphere.appserver.mpConfig-1.4) \
                                                          ))"
-bundles=com.ibm.ws.io.smallrye.reactive.messaging-provider.config10
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
