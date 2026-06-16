-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.persistenceContainer-4.0
visibility=public
singleton=true
kind=noship
edition=full
Subsystem-Name: Jakarta Persistence 4.0 Container
IBM-ShortName: persistenceContainer-4.0
WLP-AlsoKnownAs: jpaContainer-4.0
IBM-API-Package: jakarta.persistence; type="spec", \
 jakarta.persistence.spi; type="spec", \
 jakarta.persistence.criteria; type="spec", \
 jakarta.persistence.metamodel; type="spec"
IBM-App-ForceRestart: uninstall, \
 install
-features=com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  io.openliberty.xmlBinding.internal-4.0, \
  io.openliberty.jakarta.annotation-3.0; apiJar=false, \
  com.ibm.websphere.appserver.eeCompatible-12.0, \
  com.ibm.websphere.appserver.jndi-1.0, \
  io.openliberty.jakarta.persistence-4.0, \
  com.ibm.websphere.appserver.transaction-2.1
-bundles=com.ibm.ws.jpa.container.v32, \
 com.ibm.ws.jpa.container.jakarta, \
 com.ibm.ws.jpa.container.thirdparty.jakarta
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: jakartaee-12.0
