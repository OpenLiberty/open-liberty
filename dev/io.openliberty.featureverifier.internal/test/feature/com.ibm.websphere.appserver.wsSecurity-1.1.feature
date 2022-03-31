-include= ../../../../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.wsSecurity-1.1
visibility=public
IBM-API-Package: org.apache.ws.security;  type="third-party", \
 org.apache.ws.security.action;  type="third-party", \
 org.apache.ws.security.cache;  type="third-party", \
 org.apache.ws.security.components.crypto;  type="third-party", \
 org.apache.ws.security.conversation;  type="third-party", \
 org.apache.ws.security.conversation.dkalgo; type="third-party", \
 org.apache.ws.security.handler;  type="third-party", \
 org.apache.ws.security.message;  type="third-party", \
 org.apache.ws.security.message.token;  type="third-party", \
 org.apache.ws.security.processor;  type="third-party", \
 org.apache.ws.security.saml;  type="third-party", \
 org.apache.ws.security.saml.ext;  type="third-party", \
 org.apache.ws.security.saml.ext.bean;  type="third-party", \
 org.apache.ws.security.saml.ext.builder;  type="third-party", \
 org.apache.ws.security.spnego;  type="third-party", \
 org.apache.ws.security.str;  type="third-party", \
 org.apache.ws.security.transform;  type="third-party", \
 org.apache.ws.security.util;  type="third-party", \
 org.apache.ws.security.validate;  type="third-party"
IBM-ShortName: wsSecurity-1.1
Subsystem-Name: Web Service Security 1.1
-features=com.ibm.websphere.appserver.jta-1.1; ibm.tolerates:=1.2; apiJar=false, \
 com.ibm.websphere.appserver.appSecurity-2.0, \
 com.ibm.websphere.appserver.jaxws-2.2
-bundles=com.ibm.ws.org.opensaml.xmltooling.1.3.4, \
 com.ibm.ws.org.joda.time.1.6.2, \
 com.ibm.ws.org.opensaml.opensaml.2.5.3, \
 com.ibm.ws.prereq.wsdl4j.1.6.2, \
 com.ibm.ws.net.sf.ehcache.core.2.5.2, \
 com.ibm.ws.org.apache.cxf-rt-ws-mex.2.6.2, \
 com.ibm.ws.wssecurity, \
 com.ibm.ws.security.sso.common, \
 com.ibm.websphere.appserver.thirdparty.wssecurity; location:="dev/api/third-party/,lib/", \
 com.ibm.ws.org.apache.cxf.ws.security.2.6.2, \
 com.ibm.ws.org.opensaml.openws.1.4.4, \
 io.openliberty.org.apache.commons.logging, \
 com.ibm.websphere.appserver.thirdparty.wssxmlSecurity; location:="dev/api/third-party/,lib/", \
 com.ibm.websphere.appserver.api.saml20; location:="dev/api/ibm/,lib/", 
kind=ga
edition=base
