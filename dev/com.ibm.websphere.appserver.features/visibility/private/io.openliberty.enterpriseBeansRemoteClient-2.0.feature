-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.enterpriseBeansRemoteClient-2.0
visibility=private
IBM-API-Package: com.ibm.websphere.ejbcontainer; type="internal", \
 com.ibm.ws.ejb.portable; type="internal"
-features=io.openliberty.jakartaeePlatform-9.0, \
  com.ibm.websphere.appserver.iiopclient-1.0, \
  io.openliberty.ejbCore-2.0, \
  io.openliberty.jakarta.interceptor-2.0, \
  io.openliberty.jakarta.enterpriseBeans-4.0, \
  com.ibm.websphere.appserver.transaction-2.0
-bundles=com.ibm.ws.ejbcontainer.remote.client.jakarta, \
 io.openliberty.ejbcontainer.v40.internal, \
 com.ibm.ws.ejbcontainer.remote.jakarta
-files=clients/ejbRemotePortable.jakarta.jar
kind=beta
edition=base
WLP-Activation-Type: parallel
