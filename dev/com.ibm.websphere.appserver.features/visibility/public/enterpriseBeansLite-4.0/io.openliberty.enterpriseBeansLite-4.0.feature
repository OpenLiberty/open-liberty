-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.enterpriseBeansLite-4.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: enterpriseBeansLite-4.0
WLP-AlsoKnownAs: ejbLite-4.0
IBM-API-Package: com.ibm.websphere.ejbcontainer.mbean; type="ibm-api", \
 com.ibm.websphere.ejbcontainer; type="internal"
Subsystem-Category: JakartaEE9Application
-features=io.openliberty.jakartaeePlatform-9.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  com.ibm.websphere.appserver.contextService-1.0, \
  io.openliberty.ejbLiteCore-2.0, \
  io.openliberty.jakarta.interceptor-2.0, \
  io.openliberty.jakarta.enterpriseBeans-4.0, \
  com.ibm.websphere.appserver.transaction-2.0
-bundles=io.openliberty.ejbcontainer.v40.internal, \
 com.ibm.ws.ejbcontainer.timer.jakarta, \
 com.ibm.ws.ejbcontainer.async.jakarta
-jars=io.openliberty.ejbcontainer.2.0; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.ejbcontainer_1.0-javadoc.zip
Subsystem-Name: Jakarta Enterprise Beans 4.0 Lite
kind=beta
edition=core
WLP-Activation-Type: parallel
