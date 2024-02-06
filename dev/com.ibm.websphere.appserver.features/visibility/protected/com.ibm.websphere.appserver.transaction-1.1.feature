-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.transaction-1.1
WLP-DisableAllFeatures-OnConflict: false
visibility=protected
singleton=true
IBM-API-Package: javax.transaction;  type="spec", \
 javax.transaction.xa;  type="spec", \
 com.ibm.wsspi.uow;  type="ibm-api", \
 com.ibm.websphere.jtaextensions;  type="ibm-api", \
 com.ibm.websphere.uow;  type="ibm-api", \
 com.ibm.tx.jta;  type="ibm-api", \
 com.ibm.ws.Transaction.resources;  type="internal", \
 com.ibm.ws.LocalTransaction.resources;  type="internal"
IBM-SPI-Package: com.ibm.wsspi.tx
IBM-API-Service: com.ibm.wsspi.uow.UOWManager, \
 javax.transaction.TransactionSynchronizationRegistry, \
 javax.transaction.UserTransaction
-features=io.openliberty.servlet.api-3.0; apiJar=false; ibm.tolerates:="3.1", \
  com.ibm.websphere.appserver.jta-1.1, \
  com.ibm.websphere.appserver.injection-1.0, \
  com.ibm.websphere.appserver.eeCompatible-6.0; ibm.tolerates:="7.0,8.0", \
  com.ibm.websphere.appserver.javax.connector.internal-1.6
-bundles=com.ibm.ws.tx.jta.extensions, \
 com.ibm.ws.transaction; start-phase:=CONTAINER_LATE, \
 com.ibm.tx.jta, \
 com.ibm.tx.util, \
 com.ibm.rls.jdbc, \
 com.ibm.ws.tx.embeddable, \
 com.ibm.ws.recoverylog, \
 com.ibm.ws.security.auth.data.common
-jars=com.ibm.websphere.appserver.spi.transaction; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.transaction_1.1-javadoc.zip
kind=ga
edition=core
