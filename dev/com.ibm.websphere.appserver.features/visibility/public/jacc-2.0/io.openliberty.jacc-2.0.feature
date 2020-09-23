-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jacc-2.0
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: jakarta.security.jacc; type="spec", \
 com.ibm.wsspi.security.authorization.jacc; type="ibm-api"
IBM-ShortName: jacc-2.0
Subsystem-Name: Jakarta Authorization Contract for Containers 2.0
IBM-Install-Policy: when-satisfied
-features=io.openliberty.jakarta.servlet-5.0, \
 io.openliberty.appSecurity-4.0, \
 com.ibm.websphere.appserver.javaeedd-1.0, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=io.openliberty.jakarta.jacc.2.0; location:=dev/api/spec/; mavenCoordinates="jakarta.authorization:jakarta.authorization-api:2.0.0", \
 io.openliberty.security.authorization.internal.jacc, \
 com.ibm.ws.security.audit.utils
kind=beta
edition=core
-jars=io.openliberty.jacc.2.0; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.jacc_1.0-javadoc.zip
