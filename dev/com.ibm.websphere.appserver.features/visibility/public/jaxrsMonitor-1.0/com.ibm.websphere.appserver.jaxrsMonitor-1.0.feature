-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxrsMonitor-1.0
visibility=public
singleton=true
IBM-API-Package: com.ibm.websphere.jaxrs.monitor; type="ibm-api"
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: jaxrsMonitor-1.0
Subsystem-Name: Java RESTful Services Monitor 1.0
-features=com.ibm.websphere.appserver.jaxrs-2.1;ibm.tolerates:=2.0, \
com.ibm.websphere.appserver.monitor-1.0
-bundles=com.ibm.ws.jaxrs.2.x.monitor
kind=beta
edition=core
