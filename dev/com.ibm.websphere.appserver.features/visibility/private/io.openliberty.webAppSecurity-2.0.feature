-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.webAppSecurity-2.0
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: com.ibm.websphere.security.web; type="ibm-api"
-features=com.ibm.websphere.appserver.distributedMap-1.0
-bundles=com.ibm.ws.webcontainer.security.app, \
 io.openliberty.webcontainer.security.internal; start-phase:=SERVICE_EARLY, \
 com.ibm.ws.security.appbnd, \
 io.openliberty.security.authentication.internal.filter, \
 io.openliberty.security.sso.internal
-jars=io.openliberty.webcontainer.security.app; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/io.openliberty.webcontainer.security.app_1.4-javadoc.zip
kind=ga
edition=core
