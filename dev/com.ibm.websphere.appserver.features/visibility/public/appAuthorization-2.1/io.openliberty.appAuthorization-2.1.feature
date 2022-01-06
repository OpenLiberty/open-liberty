-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.appAuthorization-2.1
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: jakarta.security.jacc; type="spec", \
 com.ibm.wsspi.security.authorization.jacc; type="ibm-api"
IBM-ShortName: appAuthorization-2.1
WLP-AlsoKnownAs: jacc-2.1
Subsystem-Name: Jakarta Authorization 2.1
-features=io.openliberty.servlet.api-6.0, \
  com.ibm.websphere.appserver.javaeedd-1.0, \
  io.openliberty.appSecurity-5.0, \
  com.ibm.websphere.appserver.containerServices-1.0, \
  com.ibm.websphere.appserver.eeCompatible-10.0, \
  io.openliberty.jakarta.authorization-2.1
-bundles=\
  io.openliberty.security.authorization.internal.jacc
kind=noship
edition=full
-jars=io.openliberty.jacc; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/io.openliberty.jacc_1.0-javadoc.zip
