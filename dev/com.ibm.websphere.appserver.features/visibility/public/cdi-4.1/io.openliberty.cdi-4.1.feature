-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi-4.1
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
 jakarta.enterprise.inject.build.compatible.spi;  type="spec", \
 jakarta.enterprise.lang.model;  type="spec", \
 jakarta.enterprise.lang.model.declarations;  type="spec", \
 jakarta.enterprise.lang.model.types;  type="spec", \
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
 org.jboss.weld.context.bound;type="third-party"
IBM-SPI-Package: io.openliberty.cdi.spi;type="ibm-spi"
IBM-ShortName: cdi-4.1
Subsystem-Name: Jakarta Contexts and Dependency Injection 4.1
-features=io.openliberty.jakartaeePlatform-11.0, \
  io.openliberty.servlet.api-6.1, \
  com.ibm.websphere.appserver.contextService-1.0, \
  io.openliberty.jakarta.enterpriseBeans-4.0, \
  io.openliberty.jakarta.persistence-3.2, \
  io.openliberty.jakarta.cdi-4.1, \
  io.openliberty.jakarta.xmlWS-4.0, \
  io.openliberty.jakarta.xmlBinding-4.0, \
  io.openliberty.jakarta.annotation-3.0, \
  com.ibm.websphere.appserver.eeCompatible-11.0, \
  com.ibm.websphere.appserver.internal.slf4j-1.7, \
  io.openliberty.jakarta.pages-4.0, \
  com.ibm.websphere.appserver.transaction-2.0
-bundles=io.openliberty.org.jboss.weld6, \
 io.openliberty.org.jboss.weld6.se, \
 com.ibm.ws.org.jboss.jdeparser.1.0.0, \
 com.ibm.ws.managedobject, \
 io.openliberty.org.jboss.logging35, \
 io.openliberty.org.jboss.classfilewriter.1.3, \
 com.ibm.ws.cdi.weld.jakarta, \
 com.ibm.ws.cdi.internal.jakarta, \
 io.openliberty.cdi.4.0.internal.weld, \
 io.openliberty.cdi.4.1.internal.services.fragment, \
 com.ibm.ws.cdi.interfaces.jakarta, \
 io.openliberty.cdi.4.0.internal.interfaces, \
 io.openliberty.cdi.spi; location:="dev/spi/ibm/,lib/"
-jars=io.openliberty.cdi.4.1.thirdparty; location:="dev/api/third-party/,lib/"; mavenCoordinates="org.jboss.weld:weld-osgi-bundle:6.0.0.Alpha1"
-files=dev/api/ibm/schema/ibm-managed-bean-bnd_1_0.xsd, \
 dev/api/ibm/schema/ibm-managed-bean-bnd_1_1.xsd, \
 dev/spi/ibm/javadoc/io.openliberty.cdi.spi_1.1-javadoc.zip
kind=noship
edition=full
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
