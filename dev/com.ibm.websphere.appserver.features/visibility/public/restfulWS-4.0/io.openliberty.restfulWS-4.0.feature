-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restfulWS-4.0
visibility=public
singleton=true
IBM-API-Package: com.ibm.websphere.jaxrs.server; type="ibm-api"
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: restfulWS-4.0
WLP-AlsoKnownAs: jaxrs-4.0
Subsystem-Name: Jakarta RESTful Web Services 4.0
-features=\
  io.openliberty.restfulWSClient-4.0, \
  com.ibm.websphere.appserver.eeCompatible-11.0
-bundles=\
 io.openliberty.org.jboss.resteasy.cdi.ee11, \
 io.openliberty.org.jboss.resteasy.server.ee10
kind=noship
edition=full
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
