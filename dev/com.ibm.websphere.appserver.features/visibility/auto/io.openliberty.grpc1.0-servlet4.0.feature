-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.grpc1.0-servlet4.0
Manifest-Version: 1.0
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.grpc-1.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-4.0))"
IBM-Install-Policy: when-satisfied
-features=io.openliberty.internal.grpc-1.0
kind=ga
edition=core
WLP-Activation-Type: parallel
