-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jpa-2.1
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-API-Package: org.eclipse.persistence.descriptors.changetracking; type="internal", \
 org.eclipse.persistence.queries; type="internal", \
 org.eclipse.persistence.indirection; type="internal", \
 org.eclipse.persistence.internal.cache; type="third-party", \
 org.eclipse.persistence.internal.descriptors; type="internal", \
 org.eclipse.persistence.internal.helper; type="internal", \
 org.eclipse.persistence.internal.identitymaps; type="internal", \
 org.eclipse.persistence.internal.indirection.jdk8; type="internal", \
 org.eclipse.persistence.internal.indirection; type="internal", \
 org.eclipse.persistence.internal.jpa; type="internal", \
 org.eclipse.persistence.internal.jpa.rs.metadata.model; type="internal", \
 org.eclipse.persistence.internal.weaving; type="internal", \
 org.eclipse.persistence.jpa; type="internal", \
 org.eclipse.persistence.platform.server.was; type="internal", \
 org.eclipse.persistence.sessions; type="internal", \
 org.eclipse.persistence.internal.sessions.cdi; type="internal", \
 org.eclipse.persistence.internal.sessions; type="internal", \
 org.eclipse.persistence.annotations; type="third-party", \
 org.eclipse.persistence.config; type="third-party", \
 org.eclipse.persistence.core.descriptors; type="third-party", \
 org.eclipse.persistence.core.mappings.converters; type="third-party", \
 org.eclipse.persistence.core.mappings; type="third-party", \
 org.eclipse.persistence.core.queries; type="third-party", \
 org.eclipse.persistence.core.sessions; type="third-party", \
 org.eclipse.persistence.descriptors.copying; type="third-party", \
 org.eclipse.persistence.descriptors.invalidation; type="third-party", \
 org.eclipse.persistence.descriptors.partitioning; type="third-party", \
 org.eclipse.persistence.descriptors; type="third-party", \
 org.eclipse.persistence.dynamic; type="third-party", \
 org.eclipse.persistence.eis.interactions; type="third-party", \
 org.eclipse.persistence.eis.mappings; type="third-party", \
 org.eclipse.persistence.eis; type="third-party", \
 org.eclipse.persistence.exceptions.i18n; type="third-party", \
 org.eclipse.persistence.exceptions; type="third-party", \
 org.eclipse.persistence.expressions.spatial; type="third-party", \
 org.eclipse.persistence.expressions; type="third-party", \
 org.eclipse.persistence.history; type="third-party", \
 org.eclipse.persistence.internal.codegen; type="third-party", \
 org.eclipse.persistence.internal.core.databaseaccess; type="third-party", \
 org.eclipse.persistence.internal.core.descriptors; type="third-party", \
 org.eclipse.persistence.internal.core.helper; type="third-party", \
 org.eclipse.persistence.internal.core.queries; type="third-party", \
 org.eclipse.persistence.internal.core.sessions; type="third-party", \
 org.eclipse.persistence.internal.databaseaccess; type="third-party", \
 org.eclipse.persistence.internal.descriptors.changetracking; type="third-party", \
 org.eclipse.persistence.internal.dynamic; type="third-party", \
 org.eclipse.persistence.internal.expressions; type="third-party", \
 org.eclipse.persistence.internal.helper.linkedlist; type="third-party", \
 org.eclipse.persistence.internal.helper; type="third-party", \
 org.eclipse.persistence.internal.history; type="third-party", \
 org.eclipse.persistence.internal.indirection; type="third-party", \
 org.eclipse.persistence.internal.jpa.deployment.xml.parser; type="third-party", \
 org.eclipse.persistence.internal.jpa.deployment; type="third-party", \
 org.eclipse.persistence.internal.jpa.jdbc; type="third-party", \
 org.eclipse.persistence.internal.jpa.jpql; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.accessors.classes; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.accessors.mappings; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.accessors.objects; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.accessors; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.additionalcriteria; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.cache; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.changetracking; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.columns; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.converters; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.copypolicy; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.inheritance; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.listeners; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.locking; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.mappings; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.multitenant; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.nosql; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.partitioning; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.queries; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.sequencing; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.structures; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.tables; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.transformers; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata.xml; type="third-party", \
 org.eclipse.persistence.internal.jpa.metadata; type="third-party", \
 org.eclipse.persistence.internal.jpa.metamodel; type="third-party", \
 org.eclipse.persistence.internal.jpa.parsing.jpql.antlr; type="third-party", \
 org.eclipse.persistence.internal.jpa.parsing.jpql; type="third-party", \
 org.eclipse.persistence.internal.jpa.parsing; type="third-party", \
 org.eclipse.persistence.internal.jpa.querydef; type="third-party", \
 org.eclipse.persistence.internal.jpa.transaction; type="third-party", \
 org.eclipse.persistence.internal.jpa.weaving; type="third-party", \
 org.eclipse.persistence.internal.jpa; type="third-party", \
 org.eclipse.persistence.internal.libraries.antlr.runtime.debug; type="third-party", \
 org.eclipse.persistence.internal.libraries.antlr.runtime.misc; type="third-party", \
 org.eclipse.persistence.internal.libraries.antlr.runtime.tree; type="third-party", \
 org.eclipse.persistence.internal.libraries.antlr.runtime; type="third-party", \
 org.eclipse.persistence.internal.libraries.asm.commons; type="third-party", \
 org.eclipse.persistence.internal.libraries.asm.signature; type="third-party", \
 org.eclipse.persistence.internal.libraries.asm.tree.analysis; type="third-party", \
 org.eclipse.persistence.internal.libraries.asm.tree; type="third-party", \
 org.eclipse.persistence.internal.libraries.asm; type="third-party", \
 org.eclipse.persistence.internal.localization.i18n; type="third-party", \
 org.eclipse.persistence.internal.localization; type="third-party", \
 org.eclipse.persistence.internal.oxm.accessor; type="third-party", \
 org.eclipse.persistence.internal.oxm.conversion; type="third-party", \
 org.eclipse.persistence.internal.oxm.documentpreservation; type="third-party", \
 org.eclipse.persistence.internal.oxm.mappings; type="third-party", \
 org.eclipse.persistence.internal.oxm.record.deferred; type="third-party", \
 org.eclipse.persistence.internal.oxm.record.json; type="third-party", \
 org.eclipse.persistence.internal.oxm.record.namespaces; type="third-party", \
 org.eclipse.persistence.internal.oxm.record; type="third-party", \
 org.eclipse.persistence.internal.oxm.schema.model; type="third-party", \
 org.eclipse.persistence.internal.oxm.schema; type="third-party", \
 org.eclipse.persistence.internal.oxm.unmapped; type="third-party", \
 org.eclipse.persistence.internal.oxm; type="third-party", \
 org.eclipse.persistence.internal.platform.database; type="third-party", \
 org.eclipse.persistence.internal.queries; type="third-party", \
 org.eclipse.persistence.internal.security; type="third-party", \
 org.eclipse.persistence.internal.sequencing; type="third-party", \
 org.eclipse.persistence.internal.sessions.coordination.broadcast; type="third-party", \
 org.eclipse.persistence.internal.sessions.coordination.corba.sun; type="third-party", \
 org.eclipse.persistence.internal.sessions.coordination.corba; type="third-party", \
 org.eclipse.persistence.internal.sessions.coordination.jms; type="third-party", \
 org.eclipse.persistence.internal.sessions.coordination.rmi.iiop; type="third-party", \
 org.eclipse.persistence.internal.sessions.coordination.rmi; type="third-party", \
 org.eclipse.persistence.internal.sessions.coordination; type="third-party", \
 org.eclipse.persistence.internal.sessions.factories.model.event; type="third-party", \
 org.eclipse.persistence.internal.sessions.factories.model.log; type="third-party", \
 org.eclipse.persistence.internal.sessions.factories.model.login; type="third-party", \
 org.eclipse.persistence.internal.sessions.factories.model.platform; type="third-party", \
 org.eclipse.persistence.internal.sessions.factories.model.pool; type="third-party", \
 org.eclipse.persistence.internal.sessions.factories.model.project; type="third-party", \
 org.eclipse.persistence.internal.sessions.factories.model.property; type="third-party", \
 org.eclipse.persistence.internal.sessions.factories.model.rcm.command; type="third-party", \
 org.eclipse.persistence.internal.sessions.factories.model.rcm; type="third-party", \
 org.eclipse.persistence.internal.sessions.factories.model.sequencing; type="third-party", \
 org.eclipse.persistence.internal.sessions.factories.model.session; type="third-party", \
 org.eclipse.persistence.internal.sessions.factories.model.transport.discovery; type="third-party", \
 org.eclipse.persistence.internal.sessions.factories.model.transport.naming; type="third-party", \
 org.eclipse.persistence.internal.sessions.factories.model.transport; type="third-party", \
 org.eclipse.persistence.internal.sessions.factories.model; type="third-party", \
 org.eclipse.persistence.internal.sessions.factories; type="third-party", \
 org.eclipse.persistence.internal.sessions.remote; type="third-party", \
 org.eclipse.persistence.internal.sessions; type="third-party", \
 org.eclipse.persistence.jpa.dynamic; type="third-party", \
 org.eclipse.persistence.jpa.jpql.parser; type="third-party", \
 org.eclipse.persistence.jpa.jpql.tools.model.query; type="third-party", \
 org.eclipse.persistence.jpa.jpql.tools.model; type="third-party", \
 org.eclipse.persistence.jpa.jpql.tools.resolver; type="third-party", \
 org.eclipse.persistence.jpa.jpql.tools.spi; type="third-party", \
 org.eclipse.persistence.jpa.jpql.tools.utility.filter; type="third-party", \
 org.eclipse.persistence.jpa.jpql.tools.utility.iterable; type="third-party", \
 org.eclipse.persistence.jpa.jpql.tools.utility.iterator; type="third-party", \
 org.eclipse.persistence.jpa.jpql.tools.utility; type="third-party", \
 org.eclipse.persistence.jpa.jpql.tools; type="third-party", \
 org.eclipse.persistence.jpa.jpql.utility.filter; type="third-party", \
 org.eclipse.persistence.jpa.jpql.utility.iterable; type="third-party", \
 org.eclipse.persistence.jpa.jpql.utility.iterator; type="third-party", \
 org.eclipse.persistence.jpa.jpql.utility; type="third-party", \
 org.eclipse.persistence.jpa.jpql; type="third-party", \
 org.eclipse.persistence.jpa.metadata; type="third-party", \
 org.eclipse.persistence.logging; type="third-party", \
 org.eclipse.persistence.mappings.converters; type="third-party", \
 org.eclipse.persistence.mappings.foundation; type="third-party", \
 org.eclipse.persistence.mappings.querykeys; type="third-party", \
 org.eclipse.persistence.mappings.structures; type="third-party", \
 org.eclipse.persistence.mappings.transformers; type="third-party", \
 org.eclipse.persistence.mappings.xdb; type="third-party", \
 org.eclipse.persistence.mappings; type="third-party", \
 org.eclipse.persistence.oxm.annotations; type="third-party", \
 org.eclipse.persistence.oxm.attachment; type="third-party", \
 org.eclipse.persistence.oxm.documentpreservation; type="third-party", \
 org.eclipse.persistence.oxm.mappings.converters; type="third-party", \
 org.eclipse.persistence.oxm.mappings.nullpolicy; type="third-party", \
 org.eclipse.persistence.oxm.mappings; type="third-party", \
 org.eclipse.persistence.oxm.platform; type="third-party", \
 org.eclipse.persistence.oxm.record; type="third-party", \
 org.eclipse.persistence.oxm.schema; type="third-party", \
 org.eclipse.persistence.oxm.sequenced; type="third-party", \
 org.eclipse.persistence.oxm.unmapped; type="third-party", \
 org.eclipse.persistence.oxm; type="third-party", \
 org.eclipse.persistence.platform.database.converters; type="third-party", \
 org.eclipse.persistence.platform.database.events; type="third-party", \
 org.eclipse.persistence.platform.database.jdbc; type="third-party", \
 org.eclipse.persistence.platform.database.oracle.annotations; type="third-party", \
 org.eclipse.persistence.platform.database.oracle.jdbc; type="third-party", \
 org.eclipse.persistence.platform.database.oracle.plsql; type="third-party", \
 org.eclipse.persistence.platform.database.partitioning; type="third-party", \
 org.eclipse.persistence.platform.database; type="third-party", \
 org.eclipse.persistence.platform.server; type="third-party", \
 org.eclipse.persistence.platform.xml.jaxp; type="third-party", \
 org.eclipse.persistence.platform.xml; type="third-party", \
 org.eclipse.persistence.sequencing; type="third-party", \
 org.eclipse.persistence.services.websphere; type="third-party", \
 org.eclipse.persistence.services; type="third-party", \
 org.eclipse.persistence.sessions.broker; type="third-party", \
 org.eclipse.persistence.sessions.changesets; type="third-party", \
 org.eclipse.persistence.sessions.coordination.broadcast; type="third-party", \
 org.eclipse.persistence.sessions.coordination.corba.sun; type="third-party", \
 org.eclipse.persistence.sessions.coordination.corba; type="third-party", \
 org.eclipse.persistence.sessions.coordination.jms; type="third-party", \
 org.eclipse.persistence.sessions.coordination.rmi; type="third-party", \
 org.eclipse.persistence.sessions.coordination; type="third-party", \
 org.eclipse.persistence.sessions.factories; type="third-party", \
 org.eclipse.persistence.sessions.interceptors; type="third-party", \
 org.eclipse.persistence.sessions.remote.corba.sun; type="third-party", \
 org.eclipse.persistence.sessions.remote.rmi.iiop; type="third-party", \
 org.eclipse.persistence.sessions.remote.rmi; type="third-party", \
 org.eclipse.persistence.sessions.remote; type="third-party", \
 org.eclipse.persistence.sessions.serializers; type="third-party", \
 org.eclipse.persistence.sessions.server; type="third-party", \
 org.eclipse.persistence.tools.file; type="third-party", \
 org.eclipse.persistence.tools.profiler; type="third-party", \
 org.eclipse.persistence.tools.schemaframework; type="third-party", \
 org.eclipse.persistence.tools.tuning; type="third-party", \
 org.eclipse.persistence.tools.weaving.jpa; type="third-party", \
 org.eclipse.persistence.tools; type="third-party", \
 org.eclipse.persistence.transaction.was; type="third-party", \
 org.eclipse.persistence.transaction; type="third-party", \
 org.eclipse.persistence; type="third-party"
IBM-ShortName: jpa-2.1
Subsystem-Name: Java Persistence API 2.1
-features=com.ibm.websphere.appserver.transaction-1.2, \
  com.ibm.websphere.appserver.jpaContainer-2.1, \
  com.ibm.websphere.appserver.jdbc-4.1; ibm.tolerates:="4.2,4.3", \
  com.ibm.websphere.appserver.org.eclipse.persistence-2.6
-bundles=com.ibm.websphere.appserver.thirdparty.eclipselink; location:=dev/api/third-party/; mavenCoordinates="org.eclipse.persistence:eclipselink:2.6.0", \
 com.ibm.ws.jpa.container.eclipselink
kind=ga
edition=core
