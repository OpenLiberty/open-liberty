-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jeeMetadataContext-1.0
visibility=protected
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.contextService-1.0))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.javaee.metadata.context
kind=ga
edition=core
