-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.audit1.0.internal.ee-11.0
singleton=true
visibility = private

-features=\
  com.ibm.websphere.appserver.servlet-6.1,\
  io.openliberty.appSecurity-6.0
  
-bundles=\
  com.ibm.ws.request.probe.audit.servlet.jakarta

kind=noship
edition=full
