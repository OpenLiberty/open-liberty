-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.webCache1.0.internal.ee-12.0
singleton=true
visibility=private
-features=\
  com.ibm.websphere.appserver.servlet-6.2, \
  io.openliberty.pages-4.1
-bundles=com.ibm.ws.dynacache.web.jakarta, \
  com.ibm.ws.dynacache.web.servlet31.jakarta
kind=noship
edition=full
