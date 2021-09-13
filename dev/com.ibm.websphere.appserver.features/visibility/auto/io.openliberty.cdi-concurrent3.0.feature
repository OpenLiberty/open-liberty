-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi-concurrent3.0
visibility=private
#TODO remove temporary API package for simulating possible additions to Jakarta Concurrency
IBM-API-Package: prototype.enterprise.concurrent; type="ibm-api"
#TODO switch to EE 10 version of CDI once available
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.cdi-3.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.concurrent-3.0))"
-bundles=\
  io.openliberty.concurrent.cdi.jakarta
IBM-Install-Policy: when-satisfied
kind=noship
edition=full
