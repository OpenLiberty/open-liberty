-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jaxrs2.2-jaxb2.2
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jaxrs-2.2))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jaxb-2.2))"
-bundles=io.openliberty.org.jboss.resteasy.jaxb.provider
IBM-Install-Policy: when-satisfied
kind=noship
edition=base
