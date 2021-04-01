-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.wssec-1.1
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
-bundles=\
 com.ibm.ws.org.opensaml.xmltooling.1.4.4, \
 com.ibm.ws.org.joda.time.1.6.2, \
 com.ibm.ws.org.opensaml.opensaml.2.6.1, \
 com.ibm.ws.prereq.wsdl4j.1.6.2, \
 com.ibm.ws.net.sf.ehcache.core.2.5.2, \
 com.ibm.ws.org.apache.cxf.ws.mex.2.6.2, \
 com.ibm.ws.wssecurity, \
 com.ibm.ws.org.apache.cxf.ws.security.2.6.2, \
 com.ibm.ws.org.opensaml.openws.1.5.6, \
 com.ibm.ws.org.apache.commons.logging.1.0.3
kind=ga
edition=core
