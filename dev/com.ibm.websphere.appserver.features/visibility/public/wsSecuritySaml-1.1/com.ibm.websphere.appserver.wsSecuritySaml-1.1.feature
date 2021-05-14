-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.wsSecuritySaml-1.1
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-ShortName: wsSecuritySaml-1.1
Subsystem-Name: WSSecurity SAML 1.1
-features=com.ibm.websphere.appserver.wsSecurity-1.1, \
  com.ibm.websphere.appserver.jta-1.1; ibm.tolerates:="1.2", \
  com.ibm.websphere.appserver.jaxws-2.2; ibm.tolerates:=2.3, \
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0", \
  com.ibm.websphere.appserver.ssoCommon-1.0, \
  com.ibm.websphere.appserver.samlWeb-2.0, \
  io.openliberty.wsSecuritySaml1.1.internal.jaxws-2.2; ibm.tolerates:="2.3"
-bundles=com.ibm.ws.wssecurity.saml
kind=ga
edition=base
