-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.restfulWS-3.0
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
Subsystem-Name: Internal Java RESTful Services 3.0
-features=\
 io.openliberty.jakarta.restfulWS-3.0, \
 com.ibm.websphere.appserver.injection-2.0, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.servlet-5.0, \
 com.ibm.websphere.appserver.classloading-1.0, \
 com.ibm.websphere.appserver.eeCompatible-9.0, \
 com.ibm.websphere.appserver.org.eclipse.microprofile.config-1.4, \
 io.openliberty.jakarta.validation-3.0, \
 com.ibm.websphere.appserver.org.reactivestreams.reactive-streams-1.0, \
 com.ibm.websphere.appserver.mpConfig-1.4, \
 com.ibm.websphere.appserver.jndi-1.0
# com.ibm.websphere.appserver.globalhandler-1.0, \ # hard dependency on javax.servlet, etc.
# com.ibm.websphere.appserver.internal.optional.jaxb-2.2; ibm.tolerates:=2.3, \ # not sure about these...
# com.ibm.websphere.appserver.internal.optional.jaxws-2.2; ibm.tolerates:=2.3, \
-bundles=\
 io.openliberty.org.jboss.resteasy.common.jakarta, \ 
 com.ibm.ws.org.jboss.logging
kind=noship
edition=full
WLP-Activation-Type: parallel
