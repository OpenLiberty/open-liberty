-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.appAuthorization-2.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: jakarta.security.jacc; type="spec", \
 com.ibm.wsspi.security.authorization.jacc; type="ibm-api"
IBM-ShortName: appAuthorization-2.0
WLP-AlsoKnownAs: jacc-2.0
Subsystem-Name: Jakarta Authorization 2.0
-features=io.openliberty.servlet.api-5.0, \
  io.openliberty.appSecurity-4.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  io.openliberty.jakarta.authorization-2.0
-bundles=\
  io.openliberty.security.authorization.internal.jacc.common, \
  io.openliberty.security.authorization.internal.jacc.2.0
kind=ga
edition=core
-jars=io.openliberty.jacc; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/io.openliberty.jacc_1.0-javadoc.zip
WLP-Platform: jakartaee-9.1
WLP-InstantOn-Enabled: true; type:=beta