-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restfulWS3.0-validator3.0
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.restfulWS-3.0))"
IBM-API-Package: \
  jakarta.validation; type="spec", \
  jakarta.validation.bootstrap; type="spec", \
  jakarta.validation.constraints; type="spec", \
  jakarta.validation.constraintvalidation; type="spec", \
  jakarta.validation.executable; type="spec", \
  jakarta.validation.groups; type="spec", \
  jakarta.validation.metadata; type="spec", \
  jakarta.validation.spi; type="spec",\
  jakarta.validation.valueextraction; type="spec"
-features=\
  io.openliberty.el-4.0, \
  io.openliberty.jakarta.persistence-3.0, \
  io.openliberty.jakarta.validation-3.0
-bundles=\
  io.openliberty.org.jboss.resteasy.validator.provider.jakarta, \
  com.ibm.ws.com.fasterxml.classmate, \
  io.openliberty.org.hibernate.validator.7.0
IBM-Install-Policy: when-satisfied
kind=beta
edition=core
