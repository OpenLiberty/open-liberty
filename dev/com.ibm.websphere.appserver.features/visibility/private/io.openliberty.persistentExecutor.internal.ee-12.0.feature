-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.persistentExecutor.internal.ee-12.0
singleton=true
-features=com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3,4.1", \
  com.ibm.websphere.appserver.eeCompatible-12.0, \
  io.openliberty.concurrent-3.2, \
  com.ibm.websphere.appserver.transaction-2.1, \
  io.openliberty.jakarta.annotation-3.0, \
  io.openliberty.persistenceService-3.0, \
  io.openliberty.noShip-1.0
-bundles=com.ibm.ws.javaee.platform.defaultresource, \
 com.ibm.ws.resource, \
 com.ibm.ws.concurrent.persistent.jakarta
kind=noship
edition=full
WLP-Activation-Type: parallel
