-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.audit1.0.internal.ee-9.0
singleton=true
visibility = private

-features=\
  io.openliberty.appSecurity-4.0
  
-bundles=\
  com.ibm.ws.security.audit.file.jakarta,\
  com.ibm.ws.request.probe.audit.servlet.jakarta

kind=beta
edition=core
