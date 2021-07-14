-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.webCache1.0.internal.ee-9.0
singleton=true
visibility=private
-features=com.ibm.websphere.appserver.eeCompatible-9.0, \
  com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.pages-3.0
-bundles=com.ibm.ws.dynacache.web.jakarta
kind=noship
edition=full
