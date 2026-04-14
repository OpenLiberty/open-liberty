-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.appAuthorization-3.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: jakarta.security.jacc; type="spec"
IBM-ShortName: appAuthorization-3.0
WLP-AlsoKnownAs: jacc-3.0
Subsystem-Name: Jakarta Authorization 3.0
-features=io.openliberty.servlet.api-6.1, \
  io.openliberty.appSecurity-6.0, \
  com.ibm.websphere.appserver.eeCompatible-11.0, \
  io.openliberty.jakarta.authorization-3.0
-bundles=\
  io.openliberty.security.authorization.internal.jacc.common, \
  io.openliberty.security.authorization.internal.jacc.3.0
kind=ga
edition=core
WLP-Platform: jakartaee-11.0
WLP-InstantOn-Enabled: true
