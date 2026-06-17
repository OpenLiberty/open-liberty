-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.connectors-2.2
visibility=public
singleton=true
IBM-API-Package: \
  com.ibm.ws.jca.cm.mbean; type="ibm-api", \
  jakarta.resource; type="spec", \
  jakarta.resource.cci; type="spec", \
  jakarta.resource.spi; type="spec", \
  jakarta.resource.spi.endpoint; type="spec", \
  jakarta.resource.spi.security; type="spec", \
  jakarta.resource.spi.work; type="spec"
IBM-ShortName: connectors-2.2
WLP-AlsoKnownAs: jca-2.2
Subsystem-Name: Jakarta Connectors 2.2
Subsystem-Category: JakartaEE12Application
-features= com.ibm.websphere.appserver.eeCompatible-12.0, \
  io.openliberty.jakartaeePlatform-12.0, \
  io.openliberty.appserver.connectors-2.2, \
  io.openliberty.connectors.internal-2.2, \
  com.ibm.websphere.appserver.transaction-2.1
-bundles=com.ibm.ws.app.manager.rar, \
 com.ibm.ws.jca.1.7.jakarta
kind=noship
edition=full
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: jakartaee-12.0
