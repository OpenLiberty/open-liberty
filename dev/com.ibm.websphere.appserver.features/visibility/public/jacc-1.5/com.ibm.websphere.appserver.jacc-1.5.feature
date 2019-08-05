-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jacc-1.5
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: javax.security.jacc; type="spec", \
 com.ibm.wsspi.security.authorization.jacc; type="ibm-api"
IBM-ShortName: jacc-1.5
Subsystem-Name: Java Authorization Contract for Containers 1.5
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.javax.servlet-3.0; ibm.tolerates:="3.1, 4.0", \
 com.ibm.websphere.appserver.appSecurity-2.0; ibm.tolerates:=3.0, \
 com.ibm.websphere.appserver.javaeedd-1.0, \
 com.ibm.websphere.appserver.containerServices-1.0
-bundles=com.ibm.websphere.javaee.jacc.1.5; location:=dev/api/spec/; mavenCoordinates="javax.security.jacc:javax.security.jacc-api:1.5", \
 com.ibm.ws.security.authorization.jacc, \
 com.ibm.ws.security.audit.utils
kind=ga
edition=core
-jars=com.ibm.websphere.appserver.api.jacc; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.jacc_1.0-javadoc.zip
WLP-Activation-Type: parallel
