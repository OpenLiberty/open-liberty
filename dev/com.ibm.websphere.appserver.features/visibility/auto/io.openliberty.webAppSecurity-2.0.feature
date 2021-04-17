-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.webAppSecurity-2.0
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: com.ibm.websphere.security.web; type="ibm-api"
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-5.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.appSecurity-4.0))"
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.distributedMap-1.0, \
 com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=com.ibm.ws.webcontainer.security.app, \
 io.openliberty.webcontainer.security.internal; start-phase:=SERVICE_EARLY, \
 com.ibm.ws.security.appbnd, \
 io.openliberty.security.authentication.internal.filter, \
 io.openliberty.security.sso.internal
-jars=com.ibm.websphere.appserver.api.webcontainer.security.app; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.webcontainer.security.app_1.4-javadoc.zip
kind=beta
edition=core
