# This private impl feature corresponds to JSON-P 1.1 with the Johnzon implementation
-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsonpImpl-1.1.1
singleton=true
visibility=private
-features=com.ibm.websphere.appserver.classloading-1.0
-bundles=com.ibm.websphere.javaee.jsonp.1.1; location:="dev/api/spec/,lib/", \
 com.ibm.ws.org.apache.johnzon.core.1.1
kind=beta
edition=core
