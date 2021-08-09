-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.transaction-2.0
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
Subsystem-Name: Jakarta Transactions 2.0
-features=io.openliberty.servlet.api-5.0; apiJar=false, \
  com.ibm.websphere.appserver.javaeedd-1.0, \
  com.ibm.websphere.appserver.containerServices-1.0, \
  io.openliberty.jakarta.annotation-2.0; apiJar=false, \
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  io.openliberty.jta-2.0, \
  com.ibm.websphere.appserver.injection-2.0, \
  com.ibm.websphere.appserver.anno-2.0, \
  com.ibm.websphere.appserver.artifact-1.0, \
  io.openliberty.jakarta.cdi-3.0; apiJar=false, \
  io.openliberty.jakarta.connectors-2.0
-bundles=com.ibm.ws.tx.jta.extensions.jakarta, \
 com.ibm.ws.transaction.jakarta; start-phase:=CONTAINER_LATE, \
 com.ibm.tx.jta.jakarta, \
 com.ibm.ws.transaction.cdi.jakarta, \
 com.ibm.tx.util.jakarta, \
 com.ibm.tx.ltc.jakarta, \
 com.ibm.rls.jdbc, \
 com.ibm.ws.tx.embeddable.jakarta, \
 com.ibm.ws.recoverylog, \
 com.ibm.ws.cdi.interfaces.jakarta
-jars=com.ibm.websphere.appserver.spi.transaction; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.transaction_1.1-javadoc.zip
kind=beta
edition=core
WLP-Activation-Type: parallel
