-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.batchManagement-1.0
WLP-DisableAllFeatures-OnConflict: false
singleton:true
visibility=public
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: batchManagement-1.0
Subsystem-Name: Batch Management 1.0
-features=com.ibm.websphere.appserver.restHandler-1.0, \
  com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:="4.0, 5.0, 6.0", \
  com.ibm.websphere.appserver.transaction-1.2; ibm.tolerates:="2.0", \
  com.ibm.websphere.appserver.jdbc-4.1; ibm.tolerates:="4.0,4.2,4.3", \
  com.ibm.websphere.appserver.eeCompatible-7.0; ibm.tolerates:="8.0,9.0,10.0", \
  io.openliberty.batchManagement1.0.internal.ee-7.0; ibm.tolerates:="9.0,10.0"
-jars=com.ibm.ws.jbatch.utility, \
  com.ibm.websphere.javaee.batch.1.0; location:="dev/api/spec/", \
  com.ibm.ws.org.glassfish.json.1.0, \
  com.ibm.websphere.javaee.jsonp.1.0; location:="dev/api/spec/" 
-files=bin/batchManager.bat, \
 bin/batchManager; ibm.file.encoding:=ebcdic, \
 bin/tools/ws-jbatchutil.jar
kind=ga
edition=base
