-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.lra-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: lra-1.0
Subsystem-Name: Long Running Actions 1.0
-features=com.ibm.websphere.appserver.channelfw-1.0
-bundles=com.ibm.ws.require.java8, \
 com.ibm.ws.lra
kind=noship
edition=full
