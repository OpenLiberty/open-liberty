-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.webservicesee-1.3
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Provision-Capability: osgi.identity;filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jaxws-2.2))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.javaee.ddmodel.ws, \
 com.ibm.ws.webservices.javaee.common, \
 com.ibm.websphere.javaee.jaxws.2.2; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.xml.ws:jaxws-api:2.2.12"
kind=ga
edition=base
WLP-Activation-Type: parallel
