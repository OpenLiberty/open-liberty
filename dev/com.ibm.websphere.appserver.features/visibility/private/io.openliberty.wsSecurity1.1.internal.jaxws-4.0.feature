-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.wsSecurity1.1.internal.jaxws-4.0
visibility=private
singleton=true
-features=io.openliberty.appSecurity-5.0, \
 io.openliberty.servlet.api-6.0; apiJar=false, \
 io.openliberty.xmlWS-4.0, \
 io.openliberty.wss4j-2.3, \
 io.openliberty.jta-2.0, \
 com.ibm.websphere.appserver.httpcommons-1.0
-bundles=\
 com.ibm.ws.org.joda.time.2.9.9, \
 com.ibm.ws.org.cryptacular.cryptacular.1.2.4, \
 com.ibm.ws.org.ehcache.ehcache.107.3.8.1.jakarta, \
 com.ibm.ws.org.jasypt.jasypt.1.9.3, \
 com.ibm.ws.org.apache.cxf.rt.ws.mex.3.4.1.jakarta, \
 com.ibm.ws.org.apache.cxf.rt.ws.security.3.4.1.jakarta, \
 com.ibm.ws.org.apache.cxf.rt.security.3.4.1, \
 com.ibm.ws.org.apache.cxf.rt.security.saml.3.4.1.jakarta, \
 com.ibm.ws.wssecurity.3.4.1.jakarta
kind=noship
edition=full
