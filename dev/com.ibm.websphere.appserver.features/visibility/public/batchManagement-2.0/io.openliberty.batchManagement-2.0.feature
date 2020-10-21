-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.batchManagement-2.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-App-ForceRestart: uninstall, \
 install
IBM-API-Package: jakarta.batch.api; type="spec", \
 jakarta.batch.api.chunk; type="spec", \
 jakarta.batch.api.chunk.listener; type="spec", \
 jakarta.batch.api.listener; type="spec", \
 jakarta.batch.api.partition; type="spec", \
 jakarta.batch.operations; type="spec", \
 jakarta.batch.runtime; type="spec", \
 jakarta.batch.runtime.context; type="spec"
IBM-ShortName: batchManagement-1.0
Subsystem-Name: Batch Management 1.0
-features=com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.batch-2.0, \
  com.ibm.websphere.appserver.restHandler-1.0, \
  com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3, 4.1", \
  io.openliberty.jsonp-2.0, \
  com.ibm.websphere.appserver.transaction-2.0
-bundles=com.ibm.ws.jbatch.joblog, \
  com.ibm.ws.jbatch.rest
-jars=com.ibm.ws.jbatch.utility
-files=bin/batchManager.bat, \
 bin/batchManager; ibm.file.encoding:=ebcdic, \
 bin/tools/ws-jbatchutil.jar
kind=noship
edition=base
