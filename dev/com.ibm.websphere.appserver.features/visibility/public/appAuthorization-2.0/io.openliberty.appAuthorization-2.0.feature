-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.appAuthorization-2.0
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: jakarta.security.jacc; type="spec", \
 com.ibm.wsspi.security.authorization.jacc; type="ibm-api"
IBM-ShortName: appAuthorization-2.0
WLP-AlsoKnownAs: jacc-2.0
Subsystem-Name: Jakarta Authorization 2.0
-features=\
  io.openliberty.servlet.api-5.0, \
  io.openliberty.appSecurity-4.0, \
  io.openliberty.jakarta.authorization-2.0, \
  com.ibm.websphere.appserver.javaeedd-1.0, \
  com.ibm.websphere.appserver.containerServices-1.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=\
  io.openliberty.security.authorization.internal.jacc
kind=beta
edition=core
-jars=io.openliberty.jacc.2.0; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.jacc_1.0-javadoc.zip
