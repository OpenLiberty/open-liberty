-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.connectionManagement-1.0
visibility=private
-features=com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:=1.2, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.jcaSecurity-1.0
-bundles=com.ibm.ws.jca.cm
kind=ga
edition=core
