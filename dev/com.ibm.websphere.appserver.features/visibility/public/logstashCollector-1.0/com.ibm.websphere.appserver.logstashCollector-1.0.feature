-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.logstashCollector-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-ShortName: logstashCollector-1.0
Manifest-Version: 1.0
Subsystem-Name: Logstash Collector 1.0
-features=com.ibm.websphere.appserver.ssl-1.0
-bundles=com.ibm.ws.collector, \
 com.ibm.ws.logstash.collector, \
 com.ibm.ws.logstash.collector.1.0
kind=ga
edition=core
