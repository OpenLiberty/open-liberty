-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi-3.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: jakarta.decorator;  type="spec", \
 jakarta.enterprise.context;  type="spec", \
 jakarta.enterprise.context.control;  type="spec", \
 jakarta.enterprise.context.spi;  type="spec", \
 jakarta.enterprise.event;  type="spec", \
 jakarta.enterprise.inject;  type="spec", \
 jakarta.enterprise.inject.literal;  type="spec", \
 jakarta.enterprise.inject.spi;  type="spec", \
 jakarta.enterprise.inject.spi.configurator;  type="spec", \
 jakarta.enterprise.inject.se;  type="spec", \
 jakarta.enterprise.util;  type="spec", \
 jakarta.inject;  type="spec", \
 jakarta.interceptor;  type="spec", \
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
IBM-ShortName: cdi-3.0
Subsystem-Name: Jakarta Contexts and Dependency Injection 3.0
-features=io.openliberty.jakarta.pages-3.0, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 io.openliberty.jakarta.persistence-3.0, \
 io.openliberty.jakartaeePlatform-9.0, \
 io.openliberty.jakarta.enterpriseBeans-4.0, \
 io.openliberty.jakarta.annotation-2.0, \
 com.ibm.websphere.appserver.eeCompatible-9.0, \
 io.openliberty.jakarta.interceptor-2.0, \
 io.openliberty.jakarta.cdi-3.0, \
 com.ibm.websphere.appserver.injection-2.0, \
 com.ibm.websphere.appserver.classloading-1.0, \
 com.ibm.websphere.appserver.appmanager-1.0, \
 com.ibm.websphere.appserver.transaction-2.0, \
 io.openliberty.servlet.api-5.0, \
 io.openliberty.jakarta.xmlBinding-3.0, \
 io.openliberty.jakarta.xmlWS-3.0, \
 com.ibm.websphere.appserver.internal.slf4j-1.7.7, \
 com.ibm.websphere.appserver.contextService-1.0
-bundles=io.openliberty.org.jboss.weld4, \
 io.openliberty.org.jboss.weld4.se, \
 com.ibm.ws.org.jboss.jdeparser.1.0.0, \
 com.ibm.ws.managedobject, \
 com.ibm.ws.org.jboss.logging, \
 com.ibm.ws.org.jboss.classfilewriter.1.2, \
 com.ibm.ws.cdi.weld.jakarta, \
 com.ibm.ws.cdi.internal.jakarta, \
 com.ibm.ws.cdi.2.0.weld.jakarta, \
 com.ibm.ws.cdi.interfaces.jakarta, \
 com.ibm.websphere.appserver.spi.cdi.jakarta; location:="dev/spi/ibm/,lib/"
-jars=io.openliberty.cdi.3.0.thirdparty; location:="dev/api/third-party/,lib/"; mavenCoordinates="org.jboss.weld:weld-osgi-bundle:4.0.0"
-files=dev/api/ibm/schema/ibm-managed-bean-bnd_1_0.xsd, \
 dev/api/ibm/schema/ibm-managed-bean-bnd_1_1.xsd, \
 dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.cdi_1.1-javadoc.zip
kind=beta
edition=core
WLP-Activation-Type: parallel
