-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.persistence-4.0
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-API-Package: \
 org.hibernate; type="third-party", \
 org.hibernate.annotations; type="third-party", \
 org.hibernate.boot; type="third-party", \
 org.hibernate.boot.model; type="third-party", \
 org.hibernate.boot.model.naming; type="third-party", \
 org.hibernate.boot.model.relational; type="third-party", \
 org.hibernate.boot.registry; type="third-party", \
 org.hibernate.boot.registry.classloading.spi; type="third-party", \
 org.hibernate.boot.registry.selector; type="third-party", \
 org.hibernate.boot.registry.selector.spi; type="third-party", \
 org.hibernate.boot.spi; type="third-party", \
 org.hibernate.cache; type="third-party", \
 org.hibernate.cache.spi; type="third-party", \
 org.hibernate.cfg; type="third-party", \
 org.hibernate.context; type="third-party", \
 org.hibernate.context.spi; type="third-party", \
 org.hibernate.dialect; type="third-party", \
 org.hibernate.engine; type="third-party", \
 org.hibernate.engine.jdbc; type="third-party", \
 org.hibernate.engine.jdbc.spi; type="third-party", \
 org.hibernate.engine.spi; type="third-party", \
 org.hibernate.engine.transaction.spi; type="third-party", \
 org.hibernate.event.spi; type="third-party", \
 org.hibernate.id; type="third-party", \
 org.hibernate.integrator.spi; type="third-party", \
 org.hibernate.jpa; type="third-party", \
 org.hibernate.loader; type="third-party", \
 org.hibernate.mapping; type="third-party", \
 org.hibernate.metamodel; type="third-party", \
 org.hibernate.metamodel.spi; type="third-party", \
 org.hibernate.persister.entity; type="third-party", \
 org.hibernate.proxy; type="third-party", \
 org.hibernate.query; type="third-party", \
 org.hibernate.query.criteria; type="third-party", \
 org.hibernate.query.spi; type="third-party", \
 org.hibernate.resource.transaction.spi; type="third-party", \
 org.hibernate.service; type="third-party", \
 org.hibernate.service.spi; type="third-party", \
 org.hibernate.sql; type="third-party", \
 org.hibernate.stat; type="third-party", \
 org.hibernate.tool.schema; type="third-party", \
 org.hibernate.type; type="third-party", \
 org.hibernate.type.spi; type="third-party", \
 net.bytebuddy; type="third-party", \
 net.bytebuddy.description; type="third-party", \
 net.bytebuddy.dynamic; type="third-party", \
 net.bytebuddy.implementation; type="third-party", \
 net.bytebuddy.matcher; type="third-party", \
 org.jboss.logging; type="third-party"
IBM-ShortName: persistence-4.0
WLP-AlsoKnownAs: jpa-4.0
Subsystem-Name: Jakarta Persistence 4.0
-features=com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  io.openliberty.persistenceContainer-4.0, \
  com.ibm.websphere.appserver.eeCompatible-12.0, \
  io.openliberty.jsonp-2.2, \
  com.ibm.websphere.appserver.transaction-2.1
-bundles=io.openliberty.persistence.4.0.thirdparty; location:=dev/api/third-party/; mavenCoordinates="org.hibernate.orm:hibernate-core:8.0.0.Beta1", \
 com.ibm.ws.jpa.container.hibernate
kind=noship
edition=full
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: jakartaee-12.0
