-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.sessionDatabase1.0.jakarta
visibility = private

-bundles= com.ibm.ws.session.jakarta, \
  		  com.ibm.ws.session.db.jakarta, \
  		  com.ibm.ws.session.store.jakarta

IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.sessionDatabase-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.servlet.api-5.0))"

IBM-Install-Policy: when-satisfied

kind=noship
edition=full

