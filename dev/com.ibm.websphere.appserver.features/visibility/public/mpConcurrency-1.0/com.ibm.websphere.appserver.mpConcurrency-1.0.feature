-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpConcurrency-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: org.eclipse.microprofile.concurrent; type="stable", \
                 org.eclipse.microprofile.concurrent.spi; type="stable"
IBM-ShortName: mpConcurrency-1.0
Subsystem-Name: MicroProfile Concurrency 1.0
-features=\
  com.ibm.websphere.appserver.concurrent-1.0, \
  com.ibm.websphere.appserver.concurrent.mp-1.0
-bundles=\
  com.ibm.ws.require.java8
kind=noship
edition=full
