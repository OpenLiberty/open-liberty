-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.persistence-4.0
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-API-Package: org.hibernate; type="third-party", \
 org.hibernate.annotations; type="third-party", \
 org.hibernate.bytecode.enhance.spi; type="third-party", \
 org.hibernate.bytecode.enhance.spi.interceptor; type="third-party", \
 org.hibernate.engine.spi; type="third-party", \
 org.hibernate.jpa; type="internal", \
 org.hibernate.proxy; type="third-party", \
 org.hibernate.query; type="third-party", \
 org.hibernate.stat; type="third-party"
IBM-ShortName: persistence-4.0
WLP-AlsoKnownAs: jpa-4.0
Subsystem-Name: Jakarta Persistence 4.0
-features=com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  io.openliberty.persistenceContainer-4.0, \
  com.ibm.websphere.appserver.eeCompatible-12.0, \
  io.openliberty.jsonp-2.2, \
  com.ibm.websphere.appserver.transaction-2.1
-bundles=io.openliberty.persistence.hibernate; location:=dev/api/third-party/; mavenCoordinates="org.hibernate.orm:hibernate-core:8.0.0.Beta1", \
 com.ibm.ws.jpa.container.hibernate
kind=noship
edition=full
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: jakartaee-12.0
