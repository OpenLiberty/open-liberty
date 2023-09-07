-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restfulWS3.1-validator3.0
visibility=private
 IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.restfulWS-3.1)(osgi.identity=io.openliberty.restfulWS-4.0)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.beanValidation-3.0)(osgi.identity=io.openliberty.beanValidation-3.1)))"
-bundles=\
  io.openliberty.org.jboss.resteasy.validator.provider.ee10
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
WLP-Activation-Type: parallel
