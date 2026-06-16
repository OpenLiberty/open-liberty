-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.transaction-2.1
visibility=protected
singleton=true
IBM-API-Package: jakarta.transaction;  type="spec", \
 javax.transaction.xa;  type="spec", \
 com.ibm.wsspi.uow;  type="ibm-api", \
 com.ibm.websphere.jtaextensions;  type="ibm-api", \
 com.ibm.websphere.uow;  type="ibm-api", \
 com.ibm.tx.jta;  type="ibm-api", \
 com.ibm.ws.Transaction.resources;  type="internal", \
 com.ibm.ws.LocalTransaction.resources;  type="internal"
IBM-SPI-Package: com.ibm.wsspi.tx
IBM-API-Service: com.ibm.wsspi.uow.UOWManager, \
 jakarta.transaction.TransactionSynchronizationRegistry, \
 jakarta.transaction.UserTransaction
Subsystem-Name: Jakarta Transactions 2.1
-features=io.openliberty.servlet.api-6.2; apiJar=false, \
  io.openliberty.jakarta.annotation-3.0; apiJar=false, \
  com.ibm.websphere.appserver.eeCompatible-12.0, \
  io.openliberty.jta-2.1, \
  com.ibm.websphere.appserver.injection-2.0, \
  io.openliberty.jakarta.connectors-2.2
-bundles=com.ibm.ws.tx.jta.extensions.jakarta, \
 com.ibm.ws.transaction.jakarta; start-phase:=CONTAINER_LATE, \
 com.ibm.tx.jta.jakarta, \
 io.openliberty.transaction.internal.cdi20.jakarta, \
 com.ibm.tx.util.jakarta, \
 com.ibm.rls.jdbc.jakarta, \
 com.ibm.ws.tx.embeddable.jakarta, \
 com.ibm.ws.recoverylog, \
 com.ibm.ws.security.auth.data.common, \
 com.ibm.ws.cdi.interfaces.jakarta
-jars=com.ibm.websphere.appserver.spi.transaction; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.transaction_1.1-javadoc.zip
kind=noship
edition=full
WLP-Activation-Type: parallel
