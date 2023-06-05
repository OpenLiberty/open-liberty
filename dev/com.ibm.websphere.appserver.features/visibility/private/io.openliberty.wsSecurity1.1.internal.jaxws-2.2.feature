-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.wsSecurity1.1.internal.jaxws-2.2
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
-features=com.ibm.websphere.appserver.appSecurity-2.0; ibm.tolerates:="3.0", \
 io.openliberty.servlet.api-3.0; apiJar=false; ibm.tolerates:="3.1,4.0", \
 com.ibm.websphere.appserver.jaxws-2.2, \
 com.ibm.websphere.appserver.wss4j-2.3
-bundles=\
 com.ibm.ws.net.sf.ehcache.core.2.5.2, \
 com.ibm.ws.org.joda.time.2.9.9, \
 com.ibm.ws.org.cryptacular.cryptacular.1.2.4, \
 com.ibm.ws.org.ehcache.ehcache.107.3.8.1, \
 com.ibm.ws.net.sf.ehcache.core.2.5.2, \
 com.ibm.ws.org.jasypt.jasypt.1.9.3, \
 com.ibm.ws.org.apache.cxf.rt.ws.mex.3.4.1, \
 com.ibm.ws.org.apache.cxf.rt.ws.security.3.4.1, \
 com.ibm.ws.org.apache.cxf.rt.security.3.4.1, \
 com.ibm.ws.org.apache.cxf.rt.security.saml.3.4.1, \
 com.ibm.ws.wssecurity.3.4.1, \
 io.openliberty.jaxws.wssecurity, \
 io.openliberty.wssecurity
kind=ga
edition=base
