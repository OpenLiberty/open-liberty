-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.wsSecurity-1.1
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-API-Package:\
  org.apache.ws.security;  type="third-party", \
  org.apache.ws.security.components.crypto;  type="third-party", \
  org.apache.wss4j.common.ext; type="third-party", \
  org.apache.wss4j.common.crypto; type=third-party, \
  com.ibm.ws.wssecurity.callback;  version="1.0"; type="internal"
IBM-ShortName: wsSecurity-1.1
Subsystem-Name: Web Service Security 1.1
-features=io.openliberty.servlet.api-3.0; apiJar=false; ibm.tolerates:="3.1,4.0,5.0,6.0,6.1", \
  com.ibm.websphere.appserver.ssoCommon-1.0, \
  io.openliberty.wsSecurity1.1.internal.jaxws-2.2; ibm.tolerates:="3.0,4.0,11.0", \
  io.openliberty.org.bouncycastle
-bundles=\
  io.openliberty.org.apache.commons.logging
kind=ga
edition=base
