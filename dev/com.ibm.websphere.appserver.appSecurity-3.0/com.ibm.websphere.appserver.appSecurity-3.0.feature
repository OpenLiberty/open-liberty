-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.appSecurity-3.0
visibility=public
IBM-API-Package: javax.security.enterprise; type="spec", \
 javax.security.enterprise.authentication.mechanism.http; type="spec", \
 javax.security.enterprise.credential; type="spec", \
 javax.security.enterprise.identitystore; type="spec", \
 com.ibm.ws.security.javaeesec.cdi.beans.hash; type="spec"

IBM-ShortName: appSecurity-3.0
#IBM-SPI-Package:
Subsystem-Name: Application Security 3.0
-features=com.ibm.websphere.appserver.cdi-2.0, \
 com.ibm.websphere.appserver.el-3.0, \
 com.ibm.websphere.appserver.jaspic-1.1, \
 com.ibm.websphere.appserver.servlet-4.0
-bundles=com.ibm.websphere.javaee.security.1.0; location:=dev/api/spec/, \
 com.ibm.ws.security.javaeesec.1.0, \
 com.ibm.ws.security.javaeesec.cdi
kind=beta
edition=core
#-jars=xyz; location:=dev/spi/ibm/
#-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.javaee.security_1.0-javadoc.zip