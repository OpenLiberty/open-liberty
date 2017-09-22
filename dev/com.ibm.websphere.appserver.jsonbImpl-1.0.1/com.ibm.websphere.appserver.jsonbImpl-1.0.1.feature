# This private impl feature corresponds to JSON-B 1.0 with the Yasson implementation
-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsonbImpl-1.0.1
singleton=true
visibility=private
-features=com.ibm.websphere.appserver.jsonp-1.1, \
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.javax.cdi-1.2
-bundles=com.ibm.ws.org.eclipse.yasson.1.0, \
  com.ibm.websphere.javaee.jsonb.1.0; location:="dev/api/spec/,lib/"
kind=beta
edition=core
