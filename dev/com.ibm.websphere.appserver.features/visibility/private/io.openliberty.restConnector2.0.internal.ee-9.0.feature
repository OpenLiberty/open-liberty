-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.restConnector2.0.internal.ee-9.0
singleton=true
visibility = private

-features=\
  com.ibm.websphere.appserver.servlet-5.0

-bundles= com.ibm.ws.jmx.connector.server.rest.jakarta

kind=beta
edition=core
