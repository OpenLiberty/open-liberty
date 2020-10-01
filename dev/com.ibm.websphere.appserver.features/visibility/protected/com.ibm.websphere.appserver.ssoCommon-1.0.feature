-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.ssoCommon-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=protected
IBM-API-Package: com.ibm.websphere.security.saml2; type="ibm-api"
IBM-SPI-Package: com.ibm.wsspi.security.saml2
-bundles=com.ibm.websphere.appserver.spi.saml20; location:="dev/spi/ibm/,lib/", \
 com.ibm.websphere.appserver.api.saml20; location:="dev/api/ibm/,lib/", \
 com.ibm.ws.security.sso.common
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.saml20_1.1-javadoc.zip, \
 dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.saml20_1.0-javadoc.zip
kind=ga
edition=core
