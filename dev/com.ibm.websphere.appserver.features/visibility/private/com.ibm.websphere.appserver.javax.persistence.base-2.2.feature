-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.persistence.base-2.2
singleton=true
IBM-Process-Types: server, \
 client
-features=\
  com.ibm.websphere.appserver.javaeeCompatible-8.0
-bundles=\
  com.ibm.ws.javaee.persistence.2.2; location:=lib/
kind=ga
edition=core
