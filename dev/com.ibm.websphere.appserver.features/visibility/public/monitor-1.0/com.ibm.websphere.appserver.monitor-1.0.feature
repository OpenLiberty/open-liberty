-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.monitor-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-API-Package: com.ibm.websphere.monitor.jmx; type="ibm-api", \
 com.ibm.websphere.monitor.annotation; type="internal", \
 com.ibm.websphere.monitor.meters; type="internal", \
 com.ibm.websphere.pmi.client; type="internal", \
 com.ibm.websphere.pmi.server; type="internal", \
 com.ibm.websphere.pmi.stat; type="internal"
IBM-ShortName: monitor-1.0
Manifest-Version: 1.0
Subsystem-Name: Performance Monitoring 1.0
-features=com.ibm.websphere.appserver.containerServices-1.0
-bundles=com.ibm.ws.monitor
-jars=com.ibm.websphere.appserver.api.monitor; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.monitor_1.1-javadoc.zip
kind=ga
edition=core
