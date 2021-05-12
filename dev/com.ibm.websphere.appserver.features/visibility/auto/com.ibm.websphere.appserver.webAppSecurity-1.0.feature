-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.webAppSecurity-1.0
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: com.ibm.websphere.security.web; type="ibm-api"
IBM-Provision-Capability: osgi.identity; filter:="(|(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-3.0))(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-3.1))(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-4.0)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.appSecurity-2.0)(osgi.identity=com.ibm.websphere.appserver.appSecurity-3.0)))"
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.authFilter-1.0, \
 com.ibm.websphere.appserver.distributedMap-1.0, \
 com.ibm.websphere.appserver.eeCompatible-6.0; ibm.tolerates:="7.0,8.0"
-bundles=com.ibm.ws.webcontainer.security.app, \
 com.ibm.ws.webcontainer.security; start-phase:=SERVICE_EARLY, \
 com.ibm.ws.security.appbnd, \
 com.ibm.ws.security.sso
 -jars=com.ibm.websphere.appserver.api.webcontainer.security.app; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.webcontainer.security.app_1.4-javadoc.zip
kind=ga
edition=core
