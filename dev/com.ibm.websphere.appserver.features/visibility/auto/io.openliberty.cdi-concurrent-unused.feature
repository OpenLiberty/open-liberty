-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi-concurrent-unused
visibility=private
#TODO This exists to trick the FeatureChecker task into allowing us to introduce a
# io.openliberty.concurrent.cdi bundle that serves no other purpose than to allow us
# to create a io.openliberty.concurrent.cdi.jakarta bundle, which we need to do because
# builds are incapable of having dependencies on a jarkartized artifact such as
# com.ibm.ws.cdi.interfaces.jakarta
#TODO remove this whole file once there is a better way
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.noShip-1.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.cdi-2.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.concurrent-1.0))"
-bundles=\
  io.openliberty.concurrent.cdi
IBM-Install-Policy: when-satisfied
kind=noship
edition=full
