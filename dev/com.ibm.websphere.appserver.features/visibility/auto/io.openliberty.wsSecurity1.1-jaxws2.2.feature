-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.wsSecurity1.1-jaxws2.2
IBM-Provision-Capability:\
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.jaxws-2.2)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.wsSecurity-1.1))"
IBM-Install-Policy: when-satisfied
-features=io.openliberty.wsSecurity1.1.internal.jaxws-2.2
kind=ga
edition=base
