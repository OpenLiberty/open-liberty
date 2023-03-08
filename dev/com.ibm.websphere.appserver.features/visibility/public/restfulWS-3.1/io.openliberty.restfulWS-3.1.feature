-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restfulWS-3.1
visibility=public
singleton=true
IBM-API-Package: com.ibm.websphere.jaxrs.server; type="ibm-api", \
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: restfulWS-3.1
WLP-AlsoKnownAs: jaxrs-3.1
Subsystem-Name: Jakarta RESTful Web Services 3.1
-features=\
  io.openliberty.restfulWSClient-3.1, \
  com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=\
 io.openliberty.org.jboss.resteasy.cdi.ee10, \
 io.openliberty.org.jboss.resteasy.server.ee10
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
