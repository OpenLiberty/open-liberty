-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpFaultTolerance3.0-context
singleton=true
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.mpFaultTolerance-3.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpContextPropagation-1.0))"
IBM-Install-Policy: when-satisfied
-bundles=io.openliberty.microprofile.faulttolerance.3.0.internal.context
kind=noship
edition=full
WLP-Activation-Type: parallel
