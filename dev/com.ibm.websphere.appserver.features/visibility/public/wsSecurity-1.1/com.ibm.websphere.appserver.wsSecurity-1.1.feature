-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.wsSecurity-1.1
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-API-Package:\
  org.apache.ws.security;  type="third-party", \
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
  org.apache.ws.security.validate;  type="third-party", \
  com.ibm.ws.wssecurity.callback;  version="1.0"; type="internal"
IBM-ShortName: wsSecurity-1.1
Subsystem-Name: Web Service Security 1.1
-features=\
  com.ibm.websphere.appserver.jta-1.1; ibm.tolerates:=1.2; apiJar=false, \
  com.ibm.websphere.appserver.appSecurity-2.0; ibm.tolerates:=3.0, \
  com.ibm.websphere.appserver.jaxws-2.2; ibm.tolerates:=2.3, \
  com.ibm.websphere.appserver.ssoCommon-1.0
kind=ga
edition=base
