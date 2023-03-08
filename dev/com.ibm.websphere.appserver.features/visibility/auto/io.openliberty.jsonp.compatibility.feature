# If both jsonp and jsonpContainer features are enabled alter the customer that the container features supersedes the basic feature.
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonp.compatibility
visibility=private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.jsonp-2.1))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.jsonpContainer-2.1))"
IBM-Install-Policy: when-satisfied
-bundles=io.openliberty.jakarta.jsonp.compatibility
kind=ga
edition=core
