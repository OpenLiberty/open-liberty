-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.internal.jaxrs-3.0
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
Subsystem-Name: Internal Java RESTful Services 3.0
-features=\
 com.ibm.websphere.appserver.javax.jaxrs-2.1, \
 com.ibm.websphere.appserver.injection-1.0, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.servlet-4.0, \
 com.ibm.websphere.appserver.classloading-1.0, \
 com.ibm.websphere.appserver.globalhandler-1.0, \
 com.ibm.websphere.appserver.javaeeCompatible-8.0, \
 com.ibm.websphere.appserver.internal.optional.jaxb-2.2; ibm.tolerates:=2.3, \
 com.ibm.websphere.appserver.internal.optional.jaxws-2.2; ibm.tolerates:=2.3, \
 com.ibm.websphere.appserver.org.eclipse.microprofile.config-1.4, \
 com.ibm.websphere.appserver.javax.validation-2.0, \
 com.ibm.websphere.appserver.org.reactivestreams.reactive-streams-1.0, \
 com.ibm.websphere.appserver.mpConfig-1.4, \
 com.ibm.websphere.appserver.jndi-1.0
-bundles=\
 com.ibm.ws.org.jboss.resteasy.resteasy.core, \
 com.ibm.ws.org.jboss.resteasy.resteasy.core.spi, \
 com.ibm.ws.org.jboss.resteasy.resteasy.tracing.api,\
 com.ibm.ws.org.jboss.resteasy.resteasy.servlet.initializer, \
 com.ibm.ws.org.jboss.logging
kind=noship
edition=full
WLP-Activation-Type: parallel
