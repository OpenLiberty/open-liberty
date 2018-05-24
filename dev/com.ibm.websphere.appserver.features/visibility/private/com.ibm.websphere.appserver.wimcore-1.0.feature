-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.wimcore-1.0
-features=\
  com.ibm.websphere.appserver.ssl-1.0,\
  com.ibm.websphere.appserver.internal.jaxb-2.2
-bundles=\
  com.ibm.ws.security.wim.core, \
  com.ibm.websphere.security.wim.base
kind=ga
edition=core
