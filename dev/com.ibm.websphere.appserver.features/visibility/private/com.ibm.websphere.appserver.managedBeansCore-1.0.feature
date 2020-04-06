-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.managedBeansCore-1.0
IBM-API-Package: \
  com.ibm.ejs.container; type="internal", \
  javax.interceptor; type="spec"
-features=\
  com.ibm.websphere.appserver.injection-1.0, \
  com.ibm.websphere.appserver.optional.corba-1.5, \
  com.ibm.websphere.appserver.jndi-1.0, \
  com.ibm.websphere.appserver.containerServices-1.0, \
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.anno-1.0
-bundles=\
  com.ibm.ws.jaxrpc.stub, \
  com.ibm.ws.managedobject, \
  com.ibm.ws.ejbcontainer, \
  com.ibm.ws.javaee.dd.ejb
kind=ga
edition=core
WLP-Activation-Type: parallel
