-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.el-4.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: jakarta.el; type="spec", \
 org.apache.el;  type="internal", \
 org.apache.el.lang; type="internal", \
 org.apache.el.util; type="internal", \
 org.apache.el.stream; type="internal"
IBM-ShortName: el-4.0
Subsystem-Version: 4.0.0
Subsystem-Name: Jakarta Expression Language 4.0
-features=io.openliberty.jakarta.el-4.0, \
 com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=com.ibm.ws.org.apache.jasper.el.3.0.jakarta
kind=beta
edition=core
WLP-Activation-Type: parallel
