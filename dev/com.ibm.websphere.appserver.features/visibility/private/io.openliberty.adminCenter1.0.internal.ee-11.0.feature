-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.adminCenter1.0.internal.ee-11.0
visibility=private
singleton=true
-features=\
  com.ibm.websphere.appserver.servlet-6.1, \
  com.ibm.websphere.appserver.restConnector-2.0, \
  io.openliberty.jta-2.0, \
  io.openliberty.pages-4.0
-bundles=\
  com.ibm.ws.ui.servlet.filter.jakarta
kind=noship
edition=full
