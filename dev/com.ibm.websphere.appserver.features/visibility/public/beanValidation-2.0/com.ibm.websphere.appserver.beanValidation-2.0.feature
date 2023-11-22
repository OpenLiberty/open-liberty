-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.beanValidation-2.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-ShortName: beanValidation-2.0
Subsystem-Name: Bean Validation 2.0
IBM-App-ForceRestart: install, uninstall
IBM-API-Package: \
  javax.validation; type="spec", \
  javax.validation.bootstrap; type="spec", \
  javax.validation.constraints; type="spec", \
  javax.validation.constraintvalidation; type="spec", \
  javax.validation.executable; type="spec", \
  javax.validation.groups; type="spec", \
  javax.validation.metadata; type="spec", \
  javax.validation.spi; type="spec",\
  javax.validation.valueextraction; type="spec",\
  com.ibm.ws.beanvalidation.accessor; type="internal",\
  javax.annotation; type="spec",\
  javax.annotation.security; type="spec",\
  javax.annotation.sql; type="spec"
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
-features=com.ibm.websphere.appserver.internal.optional.jaxb-2.2, \
  com.ibm.websphere.appserver.transaction-1.2, \
  com.ibm.websphere.appserver.eeCompatible-8.0, \
  com.ibm.websphere.appserver.el-3.0, \
  com.ibm.websphere.appserver.beanValidationCore-1.0, \
  com.ibm.websphere.appserver.javax.cdi-2.0, \
  com.ibm.websphere.appserver.javax.validation-2.0
-bundles=\
  com.ibm.ws.beanvalidation.v20, \
  com.ibm.ws.org.hibernate.validator, \
  com.ibm.ws.org.jboss.logging, \
  com.ibm.ws.com.fasterxml.classmate
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
