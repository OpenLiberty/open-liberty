-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.ejbComponentMetadataDecorator-1.0
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.ejbCore-1.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jeeMetadataContext-1.0))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.javaee.metadata.context.ejb
kind=ga
edition=core
WLP-Activation-Type: parallel
