-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.appclient.appClient-2.0
visibility=private
-features=io.openliberty.jakartaeePlatform-9.0, \
  com.ibm.websphere.appserver.appmanager-1.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  com.ibm.websphere.appserver.iiopclient-1.0, \
  com.ibm.websphere.appclient.client-1.0, \
  com.ibm.websphere.appserver.injection-2.0
-bundles=com.ibm.ws.clientcontainer.jakarta, \
 com.ibm.ws.app.manager.war.jakarta, \
 com.ibm.ws.app.manager.client, \
 com.ibm.ws.managedobject
kind=beta
edition=base
WLP-Activation-Type: parallel
