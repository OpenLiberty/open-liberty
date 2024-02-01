-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.connectors.internal-2.0
singleton=true
IBM-API-Package: \
  com.ibm.ws.jca.service; type="internal", \
  jakarta.resource; type="spec", \
  jakarta.resource.cci; type="spec", \
  jakarta.resource.spi; type="spec", \
  jakarta.resource.spi.endpoint; type="spec", \
  jakarta.resource.spi.security; type="spec", \
  jakarta.resource.spi.work; type="spec"
-features=com.ibm.websphere.appserver.connectionManagement-1.0, \
  com.ibm.websphere.appserver.appmanager-1.0, \
  io.openliberty.xmlBinding-3.0, \
  com.ibm.websphere.appserver.contextService-1.0, \
  com.ibm.websphere.appserver.transaction-2.0
-bundles=\
  com.ibm.ws.jca.jakarta, \
  com.ibm.ws.jca.utils.jakarta, \
  com.ibm.ws.jca.feature.jakarta
kind=ga
edition=base
WLP-Activation-Type: parallel
