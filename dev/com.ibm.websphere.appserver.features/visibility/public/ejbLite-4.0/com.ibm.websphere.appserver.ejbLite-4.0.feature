-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.ejbLite-4.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: ejbLite-4.0
IBM-API-Package: com.ibm.websphere.ejbcontainer.mbean; type="ibm-api"
Subsystem-Category: JavaEE8Application
-features=com.ibm.websphere.appserver.jakarta.ejb-4.0, \
 com.ibm.websphere.appserver.contextService-1.0, \
 com.ibm.websphere.appserver.transaction-2.0, \
 com.ibm.websphere.appserver.javaeeCompatible-9.0, \
 com.ibm.websphere.appserver.jakarta.interceptor-2.0
Subsystem-Name: Jakarta Enterprise Beans Lite 4.0
kind=noship
edition=full
WLP-Activation-Type: parallel
