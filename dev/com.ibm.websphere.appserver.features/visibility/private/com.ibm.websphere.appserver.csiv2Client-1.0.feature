-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.csiv2Client-1.0
WLP-DisableAllFeatures-OnConflict: false
-features=com.ibm.websphere.appserver.iiopclient-1.0
-bundles=com.ibm.ws.security.csiv2.client, \
 com.ibm.ws.security.csiv2.common, \
 com.ibm.websphere.security, \
 com.ibm.websphere.security.impl
kind=ga
edition=base
