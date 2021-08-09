-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.sip.security-1.0
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: com.ibm.wsspi.security.tai.extension; type="ibm-api", \
 com.ibm.websphere.security.tai.extension; type="ibm-api"
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.sipServlet-1.1))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.appSecurity-1.0))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.sipcontainer.security, \
 com.ibm.ws.security.authentication.tai
-jars=com.ibm.websphere.appserver.api.sipServletSecurity.1.0; location:="dev/api/ibm/,lib/"
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.sipServletSecurity.1.0_1.0-javadoc.zip
kind=ga
edition=base
