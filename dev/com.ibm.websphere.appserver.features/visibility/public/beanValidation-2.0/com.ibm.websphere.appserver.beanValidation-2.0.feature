-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.beanValidation-2.0
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
  com.ibm.ws.beanvalidation.accessor; type="internal"
-features=\
  com.ibm.websphere.appserver.beanValidationCore-1.0, \
  com.ibm.websphere.appserver.el-3.0, \
  com.ibm.websphere.appserver.internal.optional.jaxb-2.2; ibm.tolerates:=2.3, \
  com.ibm.websphere.appserver.javaeeCompatible-8.0,\
  com.ibm.websphere.appserver.javax.cdi-2.0, \
  com.ibm.websphere.appserver.javax.interceptor-1.2, \
  com.ibm.websphere.appserver.javax.validation-2.0, \
  com.ibm.websphere.appserver.transaction-1.2
-bundles=\
  com.ibm.ws.beanvalidation.v20, \
  com.ibm.ws.org.hibernate.validator, \
  com.ibm.ws.org.jboss.logging, \
  com.ibm.ws.com.fasterxml.classmate
kind=ga
edition=core
WLP-Activation-Type: parallel
