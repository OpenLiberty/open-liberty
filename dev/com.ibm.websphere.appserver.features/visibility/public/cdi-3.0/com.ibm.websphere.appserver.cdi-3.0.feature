-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.cdi-3.0
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
IBM-ShortName: cdi-3.0
Subsystem-Name: Contexts and Dependency Injection 3.0
-features=com.ibm.websphere.appserver.jakarta.jsp-3.0, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.javax.persistence-2.2, \
 com.ibm.websphere.appserver.jakartaPlatform-9.0, \
 com.ibm.websphere.appserver.jakarta.ejb-4.0, \
 com.ibm.websphere.appserver.jakarta.annotation-2.0, \
 com.ibm.websphere.appserver.javaeeCompatible-8.0, \
 com.ibm.websphere.appserver.jakarta.interceptor-2.0, \
 com.ibm.websphere.appserver.jakarta.cdi-3.0, \
 com.ibm.websphere.appserver.injection-2.0, \
 com.ibm.websphere.appserver.classloading-1.0, \
 com.ibm.websphere.appserver.appmanager-1.0, \
 com.ibm.websphere.appserver.transaction-1.2, \
 com.ibm.websphere.appserver.jakarta.servlet-5.0, \
 com.ibm.websphere.appserver.internal.slf4j-1.7.7, \
 com.ibm.websphere.appserver.contextService-1.0
-bundles=com.ibm.ws.org.jboss.weld4, \
 com.ibm.ws.org.jboss.weld4.se, \
 com.ibm.ws.org.jboss.jdeparser.1.0.0, \
 com.ibm.ws.managedobject, \
 com.ibm.ws.org.jboss.logging, \
 com.ibm.ws.org.jboss.classfilewriter.1.2, \
 com.ibm.ws.cdi.weld, \
 com.ibm.ws.cdi.internal, \
 com.ibm.ws.cdi.2.0.weld, \
 com.ibm.websphere.javaee.jaxb.2.2; apiJar=false; require-java:="9"; location:="dev/api/spec/,lib/",\
 com.ibm.websphere.javaee.jaxws.2.2; apiJar=false; require-java:="9"; location:="dev/api/spec/,lib/", \
 com.ibm.ws.cdi.interfaces
-jars=com.ibm.websphere.appserver.thirdparty.cdi-3.0; location:="dev/api/third-party/,lib/"; mavenCoordinates="org.jboss.weld:weld-osgi-bundle:4.0.0.Alpha1"
-files=dev/api/ibm/schema/ibm-managed-bean-bnd_1_0.xsd, \
 dev/api/ibm/schema/ibm-managed-bean-bnd_1_1.xsd
kind=noship
edition=full
WLP-Activation-Type: parallel
