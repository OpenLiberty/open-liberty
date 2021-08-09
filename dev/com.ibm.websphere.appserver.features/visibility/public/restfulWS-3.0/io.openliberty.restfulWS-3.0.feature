-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restfulWS-3.0
visibility=public
singleton=true
IBM-API-Package: com.ibm.websphere.jaxrs.server; type="ibm-api", \
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: restfulWS-3.0
WLP-AlsoKnownAs: jaxrs-3.0
Subsystem-Name: Jakarta RESTful Web Services 3.0
-features=io.openliberty.restfulWSClient-3.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=\
 io.openliberty.org.jboss.resteasy.server.jakarta
kind=beta
edition=core
WLP-Activation-Type: parallel
