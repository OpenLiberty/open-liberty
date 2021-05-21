-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.batchManagement-1.0
WLP-DisableAllFeatures-OnConflict: false
singleton:true
visibility=public
IBM-App-ForceRestart: uninstall, \
 install
IBM-API-Package: javax.batch.api; type="spec", \
 javax.batch.api.chunk; type="spec", \
 javax.batch.api.chunk.listener; type="spec", \
 javax.batch.api.listener; type="spec", \
 javax.batch.api.partition; type="spec", \
 javax.batch.operations; type="spec", \
 javax.batch.runtime; type="spec", \
 javax.batch.runtime.context; type="spec"
IBM-ShortName: batchManagement-1.0
Subsystem-Name: Batch Management 1.0
-features=com.ibm.websphere.appserver.restHandler-1.0, \
  com.ibm.websphere.appserver.transaction-1.2, \
  com.ibm.websphere.appserver.jsonp-1.0; ibm.tolerates:="1.1", \
  com.ibm.websphere.appserver.batch-1.0, \
  com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:="4.0", \
  com.ibm.websphere.appserver.jdbc-4.1; ibm.tolerates:="4.0,4.2,4.3", \
  com.ibm.websphere.appserver.eeCompatible-7.0; ibm.tolerates:="8.0"
-bundles=com.ibm.ws.jbatch.joblog, \
  com.ibm.ws.jbatch.rest
-jars=com.ibm.ws.jbatch.utility
-files=bin/batchManager.bat, \
 bin/batchManager; ibm.file.encoding:=ebcdic, \
 bin/tools/ws-jbatchutil.jar
kind=ga
edition=base
