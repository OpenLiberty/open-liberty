-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.auditCollector1.0.internal.ee-9.0
singleton=true
visibility = private

-features=\
  com.ibm.websphere.appserver.servlet-5.0

-bundles=\
  com.ibm.ws.security.audit.source.jakarta

kind=beta
edition=core
