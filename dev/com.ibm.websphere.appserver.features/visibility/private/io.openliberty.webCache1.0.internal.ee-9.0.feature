-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.webCache1.0.internal.ee-9.0
singleton=true
visibility=private
-features=\
  com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.pages-3.0
-bundles=com.ibm.ws.dynacache.web.jakarta, \
  com.ibm.ws.dynacache.web.servlet31.jakarta
-jars=io.openliberty.webCache; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/io.openliberty.webCache_1.1-javadoc.zip
kind=ga
edition=core
