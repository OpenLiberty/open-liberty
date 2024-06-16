-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restfulWS4.0-validator3.1
visibility=private
 IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.restfulWS-4.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.beanValidation-3.1))"
-bundles=\
  io.openliberty.org.jboss.resteasy.validator.provider.ee11
IBM-Install-Policy: when-satisfied
 kind=ga
edition=core
WLP-Activation-Type: parallel
