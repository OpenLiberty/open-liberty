-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.persistenceContainer-3.0
visibility=public
singleton=true
kind=beta
edition=core
Subsystem-Name: Jakarta Persistence 3.0 Container
IBM-ShortName: persistenceContainer-3.0
WLP-AlsoKnownAs: jpaContainer-3.0
IBM-API-Package: jakarta.persistence; type="spec", \
 jakarta.persistence.spi; type="spec", \
 jakarta.persistence.criteria; type="spec", \
 jakarta.persistence.metamodel; type="spec"
IBM-App-ForceRestart: uninstall, \
 install
-features=com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  io.openliberty.xmlBinding-3.0, \
  io.openliberty.jakarta.annotation-2.0; apiJar=false, \
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.jndi-1.0, \
  io.openliberty.jakarta.persistence-3.0, \
  com.ibm.websphere.appserver.transaction-2.0
-bundles=com.ibm.ws.jpa.container.v30.jakarta, \
 com.ibm.ws.jpa.container.jakarta, \
 com.ibm.ws.jpa.container.thirdparty.jakarta
WLP-Activation-Type: parallel
