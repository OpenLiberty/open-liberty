-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.cdi-2.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: javax.decorator;  type="spec", \
 javax.enterprise.context;  type="spec", \
 javax.enterprise.context.control;  type="spec", \
 javax.enterprise.context.spi;  type="spec", \
 javax.enterprise.event;  type="spec", \
 javax.enterprise.inject;  type="spec", \
 javax.enterprise.inject.literal;  type="spec", \
 javax.enterprise.inject.spi;  type="spec", \
 javax.enterprise.inject.spi.configurator;  type="spec", \
 javax.enterprise.inject.se;  type="spec", \
 javax.enterprise.util;  type="spec", \
 javax.inject;  type="spec", \
 javax.interceptor;  type="spec", \
 org.jboss.weld.module.web.el; type="internal", \
 org.jboss.weld.module.jsf; type="internal", \
 org.jboss.weld.interceptor.proxy; type="internal", \
 org.jboss.weld.interceptor.util.proxy; type="internal", \
 org.jboss.weld.bean; type="internal", \
 org.jboss.weld.bean.proxy; type="internal", \
 org.jboss.weld.bean.proxy.util; type="internal", \
 org.jboss.weld.module.ejb; type="internal", \
 org.jboss.weld.proxy; type="internal", \
 org.jboss.weld.security; type="internal", \
 org.jboss.weld.serialization.spi; type="internal", \
 org.jboss.weld.context;type="third-party", \
 org.jboss.weld.context.api;type="third-party", \
 org.jboss.weld.context.beanstore;type="third-party", \
 org.jboss.weld.context.bound;type="third-party", \
 org.jboss.weld.context.conversation;type="third-party"
IBM-SPI-Package: io.openliberty.cdi.spi;type="ibm-spi"
IBM-ShortName: cdi-2.0
Subsystem-Name: Contexts and Dependency Injection 2.0
-features=com.ibm.websphere.appserver.javax.jsp-2.3, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.javax.persistence-2.2, \
 com.ibm.websphere.appserver.javaeePlatform-7.0, \
 com.ibm.websphere.appserver.javax.ejb-3.2, \
 com.ibm.websphere.appserver.javax.annotation-1.3, \
 com.ibm.websphere.appserver.eeCompatible-8.0, \
 com.ibm.websphere.appserver.javax.interceptor-1.2, \
 com.ibm.websphere.appserver.javax.cdi-2.0, \
 com.ibm.websphere.appserver.injection-1.0, \
 com.ibm.websphere.appserver.classloading-1.0, \
 com.ibm.websphere.appserver.appmanager-1.0, \
 com.ibm.websphere.appserver.transaction-1.2, \
 io.openliberty.servlet.api-4.0, \
 com.ibm.websphere.appserver.internal.slf4j-1.7.7, \
 com.ibm.websphere.appserver.contextService-1.0
-bundles=com.ibm.ws.org.jboss.weld3, \
 com.ibm.ws.org.jboss.jdeparser.1.0.0, \
 com.ibm.ws.managedobject, \
 com.ibm.ws.org.jboss.logging, \
 com.ibm.ws.org.jboss.classfilewriter.1.2, \
 com.ibm.ws.cdi.weld, \
 com.ibm.ws.cdi.internal, \
 com.ibm.ws.cdi.2.0.weld, \
 com.ibm.websphere.javaee.jaxb.2.2; apiJar=false; require-java:="9"; location:="dev/api/spec/,lib/",\
 com.ibm.websphere.javaee.jaxws.2.2; apiJar=false; require-java:="9"; location:="dev/api/spec/,lib/", \
 com.ibm.ws.cdi.interfaces, \
 com.ibm.websphere.appserver.spi.cdi; location:="dev/spi/ibm/,lib/"
-jars=com.ibm.websphere.appserver.thirdparty.cdi-2.0; location:="dev/api/third-party/,lib/"; mavenCoordinates="org.jboss.weld:weld-osgi-bundle:3.0.3.Final"
-files=dev/api/ibm/schema/ibm-managed-bean-bnd_1_0.xsd, \
 dev/api/ibm/schema/ibm-managed-bean-bnd_1_1.xsd, \
 dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.cdi_1.1-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
