# This private impl feature corresponds to JSON-B 1.0 with the Johnzon implementation
-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsonbImpl-1.0.1
singleton=true
visibility=private
-features=com.ibm.websphere.appserver.jsonp-1.1, \
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.javax.cdi-1.2
-bundles=com.ibm.websphere.javaee.jsonb.1.0;location:="dev/api/spec/,lib/",\
  com.ibm.websphere.javaee.jaxrs.2.1;location:="dev/api/spec/,lib/",\
  com.ibm.ws.org.apache.johnzon.jsonb.1.1,\
  com.ibm.ws.org.apache.johnzon.mapper.1.1
kind=beta
edition=core
