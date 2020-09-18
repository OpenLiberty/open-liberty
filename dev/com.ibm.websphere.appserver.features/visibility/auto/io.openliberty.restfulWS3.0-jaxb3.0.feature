-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restfulWS3.0-jaxb3.0
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.restfulWS-3.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.jaxb-3.0))"
-bundles=io.openliberty.org.jboss.resteasy.jaxb.provider.jakarta
IBM-Install-Policy: when-satisfied
kind=noship
edition=base
