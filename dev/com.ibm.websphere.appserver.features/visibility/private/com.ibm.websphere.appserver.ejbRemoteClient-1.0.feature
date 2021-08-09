-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.ejbRemoteClient-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=private
IBM-API-Package: com.ibm.websphere.ejbcontainer; type="internal"
-features=com.ibm.websphere.appserver.transaction-1.2, \
  com.ibm.websphere.appserver.ejbCore-1.0, \
  com.ibm.websphere.appserver.iiopclient-1.0, \
  com.ibm.websphere.appserver.javax.ejb-3.2, \
  com.ibm.websphere.appserver.javaeePlatform-7.0, \
  com.ibm.websphere.appserver.javax.interceptor-1.2
-bundles=com.ibm.ws.ejbcontainer.remote.client, \
 com.ibm.ws.ejbcontainer.v32, \
 com.ibm.ws.ejbcontainer.remote
-files=clients/ejbRemotePortable.jar
kind=ga
edition=base
