-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.messaging-3.1
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall
IBM-API-Package: jakarta.jms; version="3.1"; type="spec", \
  com.ibm.ws.jca.cm.mbean; type="ibm-api", \
  jakarta.resource; type="spec", \
  jakarta.resource.cci; type="spec", \
  jakarta.resource.spi; type="spec", \
  jakarta.resource.spi.endpoint; type="spec", \
  jakarta.resource.spi.security; type="spec", \
  jakarta.resource.spi.work; type="spec"
IBM-ShortName: messaging-3.1
WLP-AlsoKnownAs: jms-3.1
Subsystem-Name: Jakarta Messaging 3.1
-features=io.openliberty.messaging.internal-3.1, \
  com.ibm.websphere.appserver.eeCompatible-10.0; ibm.tolerates:="11.0", \
  io.openliberty.connectors-2.1, \
  com.ibm.websphere.appserver.transaction-2.0
-bundles=com.ibm.ws.jms20.feature
kind=ga
edition=base
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true; type:=beta
