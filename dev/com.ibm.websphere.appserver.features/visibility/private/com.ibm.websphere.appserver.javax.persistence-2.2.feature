-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.persistence-2.2
singleton=true
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.javax.persistence.base-2.2
-bundles=com.ibm.ws.javaee.persistence.api.2.2
-jars=com.ibm.websphere.javaee.persistence.2.2; location:=dev/api/spec/
kind=beta
edition=core
