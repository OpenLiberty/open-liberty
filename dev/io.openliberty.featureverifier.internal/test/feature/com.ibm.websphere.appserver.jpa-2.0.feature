-include= ../../../../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jpa-2.0
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-API-Package: javax.persistence; type="spec", \
 javax.persistence.spi; type="spec", \
 javax.persistence.criteria; type="spec", \
 javax.persistence.metamodel; type="spec", \
 com.ibm.websphere.persistence; type="internal", \
 com.ibm.websphere.persistence.conf; type="internal", \
 com.ibm.ws.persistence.jdbc.kernel; type="internal", \
 org.apache.openjpa.conf;  type="internal", \
 org.apache.openjpa.enhance;  type="internal", \
 org.apache.openjpa.kernel;  type="internal", \
 org.apache.openjpa.lib.log;  type="internal", \
 org.apache.openjpa.lib.rop;  type="internal", \
 org.apache.openjpa.persistence;  type="internal", \
 org.apache.openjpa.util;  type="internal", \
 org.apache.openjpa.conf;  type="third-party", \
 org.apache.openjpa.abstractstore;  type="third-party", \
 org.apache.openjpa.ant;  type="third-party", \
 org.apache.openjpa.audit;  type="third-party", \
 org.apache.openjpa.datacache;  type="third-party", \
 org.apache.openjpa.ee;  type="third-party", \
 org.apache.openjpa.enhance;  type="third-party", \
 org.apache.openjpa.event;  type="third-party", \
 org.apache.openjpa.instrumentation;  type="third-party", \
 org.apache.openjpa.instrumentation.jmx;  type="third-party", \
 org.apache.openjpa.jdbc.ant;  type="third-party", \
 org.apache.openjpa.jdbc.conf;  type="third-party", \
 org.apache.openjpa.jdbc.identifier;  type="third-party", \
 org.apache.openjpa.jdbc.kernel;  type="third-party", \
 org.apache.openjpa.jdbc.kernel.exps;  type="third-party", \
 org.apache.openjpa.jdbc.meta;  type="third-party", \
 org.apache.openjpa.jdbc.meta.strats;  type="third-party", \
 org.apache.openjpa.jdbc.schema;  type="third-party", \
 org.apache.openjpa.jdbc.sql;  type="third-party", \
 org.apache.openjpa.kernel;  type="third-party", \
 org.apache.openjpa.kernel.exps;  type="third-party", \
 org.apache.openjpa.kernel.jpql;  type="third-party", \
 org.apache.openjpa.lib.ant;  type="third-party", \
 org.apache.openjpa.lib.conf;  type="third-party", \
 org.apache.openjpa.lib.encryption;  type="third-party", \
 org.apache.openjpa.lib.graph;  type="third-party", \
 org.apache.openjpa.lib.identifier;  type="third-party", \
 org.apache.openjpa.lib.instrumentation;  type="third-party", \
 org.apache.openjpa.lib.jdbc;  type="third-party", \
 org.apache.openjpa.lib.log;  type="third-party", \
 org.apache.openjpa.lib.meta;  type="third-party", \
 org.apache.openjpa.lib.rop;  type="third-party", \
 org.apache.openjpa.lib.util;  type="third-party", \
 org.apache.openjpa.lib.util.concurrent;  type="third-party", \
 org.apache.openjpa.lib.util.svn;  type="third-party", \
 org.apache.openjpa.lib.xml;  type="third-party", \
 org.apache.openjpa.meta;  type="third-party", \
 org.apache.openjpa.persistence;  type="third-party", \
 org.apache.openjpa.persistence.criteria;  type="third-party", \
 org.apache.openjpa.persistence.jdbc;  type="third-party", \
 org.apache.openjpa.persistence.meta;  type="third-party", \
 org.apache.openjpa.persistence.osgi;  type="third-party", \
 org.apache.openjpa.persistence.query;  type="third-party", \
 org.apache.openjpa.persistence.util;  type="third-party", \
 org.apache.openjpa.persistence.validation;  type="third-party", \
 org.apache.openjpa.slice;  type="third-party", \
 org.apache.openjpa.slice.jdbc;  type="third-party", \
 org.apache.openjpa.util;  type="third-party", \
 org.apache.openjpa.validation;  type="third-party", \
 org.apache.openjpa.xmlstore;  type="third-party"
IBM-ShortName: jpa-2.0
Subsystem-Name: Java Persistence API 2.0
-features=com.ibm.websphere.appserver.jndi-1.0, \
 com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:=1.2, \
 com.ibm.websphere.appserver.javax.persistence-2.0, \
 com.ibm.websphere.appserver.classloading-1.0, \
 com.ibm.websphere.appserver.javax.annotation-1.1; ibm.tolerates:=1.2; apiJar=false, \
 com.ibm.websphere.appserver.beanValidation-1.0; ibm.tolerates:=1.1, \
 com.ibm.websphere.appserver.jpaApiStub-2.0, \
 com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:=3.1, \
 com.ibm.websphere.appserver.jdbc-4.0; ibm.tolerates:=4.1
-bundles=com.ibm.ws.jpa.container, \
 com.ibm.ws.jpa, \
 com.ibm.ws.org.apache.commons.lang.2.4, \
 com.ibm.ws.jpa.container.beanvalidation, \
 com.ibm.ws.net.sourceforge.serp.1.15.1, \
 com.ibm.ws.jpa.container.wsjpa, \
 com.ibm.ws.org.apache.commons.pool.1.5.4, \
 com.ibm.ws.org.apache.commons.collections, \
 com.ibm.websphere.appserver.thirdparty.jpa; location:="dev/api/third-party/,lib/"; mavenCoordinates="org.apache.openjpa:openjpa:2.2.0"
kind=ga
edition=core
