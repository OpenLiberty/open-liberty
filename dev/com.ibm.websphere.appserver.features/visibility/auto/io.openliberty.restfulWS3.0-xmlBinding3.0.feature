-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restfulWS3.0-xmlBinding3.0
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.restfulWSClient-3.0)(osgi.identity=io.openliberty.restfulWSClient-3.1)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.xmlBinding-3.0)(osgi.identity=io.openliberty.xmlBinding-4.0)))"
-bundles=io.openliberty.org.jboss.resteasy.jaxb.provider.jakarta
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
WLP-Activation-Type: parallel
