-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.ejbLite-3.2
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: ejbLite-3.2
IBM-API-Package: com.ibm.websphere.ejbcontainer.mbean; type="ibm-api"
Subsystem-Category: JavaEE7Application
-features=com.ibm.websphere.appserver.javaeePlatform-7.0, \
 com.ibm.websphere.appserver.javax.ejb-3.2, \
 com.ibm.websphere.appserver.ejbLiteCore-1.0, \
 com.ibm.websphere.appserver.contextService-1.0, \
 com.ibm.websphere.appserver.transaction-1.2, \
 com.ibm.websphere.appserver.javaeeCompatible-7.0; ibm.tolerates:=8.0, \
 com.ibm.websphere.appserver.javax.interceptor-1.2
-bundles=com.ibm.ws.ejbcontainer.v32, \
 com.ibm.ws.ejbcontainer.timer, \
 com.ibm.ws.ejbcontainer.async
-jars=com.ibm.websphere.appserver.api.ejbcontainer; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.ejbcontainer_1.0-javadoc.zip
Subsystem-Name: Enterprise JavaBeans Lite 3.2
kind=ga
edition=core
WLP-Activation-Type: parallel
