-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.restConnector2.0.internal.ee-9.0
singleton=true
visibility = private

-features=\
  com.ibm.websphere.appserver.servlet-5.0; ibm.tolerates:="6.0, 6.1"

-bundles= com.ibm.ws.jmx.connector.server.rest.jakarta

kind=ga
edition=core
