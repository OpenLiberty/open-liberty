-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.osgiConsole-1.0
visibility=public
IBM-ShortName: osgiConsole-1.0
IBM-Process-Types: client, \
 server
Subsystem-Name: OSGi Debug Console
-bundles=com.ibm.ws.org.apache.felix.gogo.command.0.16.0; start-phase:=SERVICE_EARLY, \
 com.ibm.ws.org.eclipse.equinox.console; start-phase:=SERVICE_EARLY, \
 com.ibm.ws.org.apache.felix.gogo.shell.0.12.0; start-phase:=SERVICE_EARLY, \
 com.ibm.ws.org.apache.felix.gogo.runtime.0.16.2; start-phase:=SERVICE_EARLY
kind=ga
edition=core
