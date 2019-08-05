-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jms-2.0
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall
IBM-API-Package: javax.jms; version="2.0"; type="spec"
IBM-ShortName: jms-2.0
Subsystem-Name: Java Message Service 2.0
-features=com.ibm.websphere.appserver.internal.jms-2.0, \
 com.ibm.websphere.appserver.transaction-1.2, \
 com.ibm.websphere.appserver.jca-1.7
-bundles=com.ibm.ws.jms20.feature
kind=ga
edition=base
WLP-Activation-Type: parallel
