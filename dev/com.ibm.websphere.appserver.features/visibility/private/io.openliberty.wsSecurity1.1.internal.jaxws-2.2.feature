-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.wsSecurity1.1.internal.jaxws-2.2
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
-features=com.ibm.websphere.appserver.appSecurity-2.0; ibm.tolerates:="3.0", \
 io.openliberty.servlet.api-3.0; apiJar=false; ibm.tolerates:="3.1,4.0", \
 com.ibm.websphere.appserver.jaxws-2.2, \
 com.ibm.websphere.appserver.wss4j-1.0, \
 com.ibm.websphere.appserver.internal.slf4j-1.7
-bundles=\
 com.ibm.ws.org.opensaml.xmltooling.1.4.4, \
 com.ibm.ws.org.joda.time.1.6.2, \
 com.ibm.ws.org.opensaml.opensaml.2.6.1, \
 com.ibm.ws.prereq.wsdl4j.1.6.2, \
 com.ibm.ws.net.sf.ehcache.core.2.5.2, \
 com.ibm.ws.org.apache.cxf.ws.mex.2.6.2, \
 com.ibm.ws.wssecurity, \
 com.ibm.ws.org.apache.cxf.ws.security.2.6.2, \
 com.ibm.ws.org.opensaml.openws.1.5.6
kind=ga
edition=base
