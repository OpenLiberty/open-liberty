-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.ejbCore-1.0
IBM-API-Package: javax.ejb; type="spec", \
 javax.ejb.embeddable; type="spec", \
 javax.ejb.spi; type="spec"
-features=com.ibm.websphere.appserver.appmanager-1.0, \
 com.ibm.websphere.appserver.javaeePlatform-6.0, \
 com.ibm.websphere.appserver.managedBeansCore-1.0
-bundles=com.ibm.ws.app.manager.war, \
 com.ibm.ws.app.manager.ejb
-files=dev/api/ibm/schema/ibm-common-bnd_1_2.xsd, \
 dev/api/ibm/schema/ibm-application-ext_1_0.xsd, \
 dev/api/ibm/schema/ibm-ejb-jar-ext_1_1.xsd, \
 dev/api/ibm/schema/ibm-ejb-jar-bnd_1_1.xsd, \
 dev/api/ibm/schema/ibm-ejb-jar-ext_1_0.xsd, \
 dev/api/ibm/schema/ibm-application-bnd_1_2.xsd, \
 dev/api/ibm/schema/ibm-application-bnd_1_0.xsd, \
 dev/api/ibm/schema/ibm-common-ext_1_0.xsd, \
 dev/api/ibm/schema/ibm-common-bnd_1_0.xsd, \
 dev/api/ibm/schema/ibm-common-ext_1_1.xsd, \
 dev/api/ibm/schema/ibm-application-bnd_1_1.xsd, \
 dev/api/ibm/schema/ibm-application-ext_1_1.xsd, \
 dev/api/ibm/schema/ibm-ejb-jar-bnd_1_2.xsd, \
 dev/api/ibm/schema/ibm-ejb-jar-bnd_1_0.xsd, \
 dev/api/ibm/schema/ibm-common-bnd_1_1.xsd
kind=ga
edition=core
