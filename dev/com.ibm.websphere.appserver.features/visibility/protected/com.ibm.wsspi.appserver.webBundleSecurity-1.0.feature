-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.wsspi.appserver.webBundleSecurity-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=protected
-features=com.ibm.websphere.appserver.security-1.0, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.distributedMap-1.0, \
 com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0,5.0", \
 com.ibm.wsspi.appserver.webBundle-1.0, \
 com.ibm.websphere.appserver.authFilter-1.0, \
 io.openliberty.webBundleSecurity1.0.internal.ee-6.0; ibm.tolerates:="9.0"
-bundles=com.ibm.websphere.security, \
 com.ibm.ws.webcontainer.security.feature, \
 com.ibm.ws.security.authorization.builtin
kind=ga
edition=core
