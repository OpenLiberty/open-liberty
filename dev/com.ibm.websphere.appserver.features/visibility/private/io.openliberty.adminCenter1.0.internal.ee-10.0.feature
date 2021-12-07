-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.adminCenter1.0.internal.ee-10.0
visibility=private
singleton=true
-features=\
  com.ibm.websphere.appserver.servlet-6.0, \
  com.ibm.websphere.appserver.restConnector-2.0, \
  io.openliberty.jta-2.0, \
  io.openliberty.pages-3.1
-bundles=\
  com.ibm.ws.ui.jakarta, \
  com.ibm.ws.org.owasp.esapi.2.1.0.jakarta
kind=noship
edition=full
