-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.transaction-1.2
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
-features=com.ibm.websphere.appserver.javax.connector.internal-1.7, \
 com.ibm.websphere.appserver.javax.cdi-1.2; ibm.tolerates:="2.0"; apiJar=false, \
 com.ibm.websphere.appserver.jta-1.2, \
 com.ibm.websphere.appserver.injection-1.0, \
 com.ibm.websphere.appserver.javax.servlet-3.0; ibm.tolerates:="3.1,4.0"; apiJar=false, \
 com.ibm.websphere.appserver.artifact-1.0, \
 com.ibm.websphere.appserver.javaeedd-1.0, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.javax.annotation-1.2; ibm.tolerates:=1.3; apiJar=false, \
 com.ibm.websphere.appserver.anno-1.0
-bundles=com.ibm.ws.tx.jta.extensions, \
 com.ibm.ws.transaction; start-phase:=CONTAINER_LATE, \
 com.ibm.tx.jta, \
 com.ibm.ws.transaction.cdi, \
 com.ibm.tx.util, \
 com.ibm.tx.ltc, \
 com.ibm.rls.jdbc, \
 com.ibm.ws.tx.embeddable, \
 com.ibm.ws.recoverylog, \
 com.ibm.ws.cdi.interfaces
-jars=com.ibm.websphere.appserver.spi.transaction; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.transaction_1.1-javadoc.zip
kind=ga
edition=core
