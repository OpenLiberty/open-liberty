-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.lra.coordinator-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: lraCoordinator-1.0
Subsystem-Name: Long Running Actions Coordinator 1.0
-bundles=com.ibm.ws.require.java8, \
         com.ibm.ws.org.jboss.narayana.rts.lra.coordinator
kind=noship
edition=full
