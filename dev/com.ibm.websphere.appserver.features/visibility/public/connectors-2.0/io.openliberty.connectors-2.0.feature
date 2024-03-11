-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.connectors-2.0
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
IBM-ShortName: connectors-2.0
WLP-AlsoKnownAs: jca-2.0
Subsystem-Name: Jakarta Connectors 2.0
Subsystem-Category: JakartaEE9Application
-features=io.openliberty.appserver.connectors-2.0, \
  io.openliberty.jakartaeePlatform-9.0, \
  io.openliberty.connectors.internal-2.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  com.ibm.websphere.appserver.transaction-2.0
-bundles=com.ibm.ws.app.manager.rar, \
 com.ibm.ws.jca.1.7.jakarta
kind=ga
edition=base
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true; type:=beta
