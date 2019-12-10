-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appclient.client-1.0
visibility=private
IBM-Process-Types: client, \
 server
-jars=com.ibm.ws.appclient.boot, \
 com.ibm.ws.kernel.boot
-files=bin/client; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 bin/client.bat, \
 bin/tools/ws-client.jar
kind=ga
edition=base
WLP-Activation-Type: parallel
