-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.beanValidation-3.1
visibility=public
singleton=true
IBM-ShortName: beanValidation-3.1
Subsystem-Name: Jakarta Validation 3.1
IBM-App-ForceRestart: install, uninstall
IBM-API-Package: \
  jakarta.validation; type="spec", \
  jakarta.validation.bootstrap; type="spec", \
  jakarta.validation.constraints; type="spec", \
  jakarta.validation.constraintvalidation; type="spec", \
  jakarta.validation.executable; type="spec", \
  jakarta.validation.groups; type="spec", \
  jakarta.validation.metadata; type="spec", \
  jakarta.validation.spi; type="spec",\
  jakarta.validation.valueextraction; type="spec",\
  com.ibm.ws.beanvalidation.accessor; type="internal",\
  jakarta.annotation; type="spec",\
  jakarta.annotation.security; type="spec",\
  jakarta.annotation.sql; type="spec"
IBM-SPI-Package: \
  com.ibm.wsspi.adaptable.module,\
  com.ibm.ws.adaptable.module.structure,\
  com.ibm.wsspi.adaptable.module.adapters,\
  com.ibm.wsspi.artifact,\
  com.ibm.wsspi.artifact.factory,\
  com.ibm.wsspi.artifact.factory.contributor,\
  com.ibm.wsspi.artifact.overlay,\
  com.ibm.wsspi.artifact.equinox.module,\
  com.ibm.wsspi.anno.classsource,\
  com.ibm.wsspi.anno.info,\
  com.ibm.wsspi.anno.service,\
  com.ibm.wsspi.anno.targets,\
  com.ibm.wsspi.anno.util,\
  com.ibm.ws.anno.classsource.specification
-features=com.ibm.websphere.appserver.eeCompatible-11.0, \
  io.openliberty.jakarta.validation-3.1, \
  io.openliberty.beanValidationCore-2.0, \
  com.ibm.websphere.appserver.transaction-2.0, \
  io.openliberty.expressionLanguage-6.0, \
  io.openliberty.jakarta.cdi-4.1
-bundles=\
  com.ibm.ws.beanvalidation.v20.jakarta, \
  com.ibm.ws.org.jboss.logging, \
  com.ibm.ws.com.fasterxml.classmate, \
  io.openliberty.org.hibernate.validator.7.0
kind=noship
edition=full
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
