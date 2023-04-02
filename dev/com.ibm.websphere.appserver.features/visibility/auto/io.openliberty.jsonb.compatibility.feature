# If both jsonb and jsonbContainer features are enabled alert the customer that the container features supersedes the basic feature.
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonb.compatibility
visibility=private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.jsonb-3.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.jsonbContainer-3.0))"
IBM-Install-Policy: when-satisfied
-bundles=io.openliberty.jakarta.jsonb.compatibility
kind=ga
edition=core
