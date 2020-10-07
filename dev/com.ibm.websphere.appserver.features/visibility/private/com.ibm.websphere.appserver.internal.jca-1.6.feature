-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.internal.jca-1.6
IBM-API-Package: \
  com.ibm.ws.jca.service; type="internal", \
  javax.resource; type="spec", \
  javax.resource.cci; type="spec", \
  javax.resource.spi; type="spec", \
  javax.resource.spi.endpoint; type="spec", \
  javax.resource.spi.security; type="spec", \
  javax.resource.spi.work; type="spec"
-features=\
  com.ibm.websphere.appserver.appmanager-1.0, \
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.connectionManagement-1.0,\
  com.ibm.websphere.appserver.contextService-1.0, \
  com.ibm.websphere.appserver.dynamicBundle-1.0, \
  com.ibm.websphere.appserver.internal.optional.jaxb-2.2; ibm.tolerates:=2.3, \
  com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:=1.2
-bundles=\
  com.ibm.ws.jca, \
  com.ibm.ws.jca.utils, \
  com.ibm.ws.jca.feature
kind=ga
edition=base
WLP-Activation-Type: parallel
