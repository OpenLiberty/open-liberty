-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.restHandler1.0.internal.ee-10.0
singleton=true
visibility = private

-features=\
  io.openliberty.servlet.internal-6.0; ibm.tolerates:="6.1", \
  com.ibm.websphere.appserver.adminSecurity-2.0

-bundles= com.ibm.ws.rest.handler.jakarta

kind=ga
edition=core

