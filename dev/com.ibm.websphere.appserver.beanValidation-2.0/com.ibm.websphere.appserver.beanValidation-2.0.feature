-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.beanValidation-2.0
visibility=public
singleton=true
IBM-ShortName: beanValidation-2.0
Subsystem-Name: Bean Validation 2.0
IBM-App-ForceRestart: install, uninstall
IBM-API-Package: javax.validation; type="spec", \
  javax.validation.bootstrap; type="spec", \
  javax.validation.constraints; type="spec", \
  javax.validation.constraintvalidation; type="spec", \
  javax.validation.executable; type="spec", \
  javax.validation.groups; type="spec", \
  javax.validation.metadata; type="spec", \
  javax.validation.spi; type="spec",\
  javax.validation.valueextraction; type="spec",\
  com.ibm.ws.beanvalidation.accessor; type="internal",\
  org.hibernate.validator; type="internal",\
  com.ibm.ws.beanvalidation.v20.cdi.internal; type="internal",\
  org.hibernate.validator.internal.engine; type="internal"
-features=\
  com.ibm.websphere.appserver.javax.cdi-2.0, \
  com.ibm.websphere.appserver.beanValidationCore-1.0, \
  com.ibm.websphere.appserver.javax.validation-2.0, \
  com.ibm.websphere.appserver.el-3.0, \
  com.ibm.websphere.appserver.transaction-1.2, \
  com.ibm.websphere.appserver.javax.interceptor-1.2, \
  com.ibm.websphere.appserver.javaeeCompatible-8.0
-bundles=\
  com.ibm.ws.beanvalidation.v20, \
  com.ibm.ws.org.hibernate.validator.6.0.4.Final, \
  com.ibm.ws.org.jboss.logging.3.3.0, \
  com.ibm.ws.com.fasterxml.classmate.1.3.1
kind=beta
edition=core
