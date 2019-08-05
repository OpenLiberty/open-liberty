-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.el-3.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: javax.el; type="spec", \
 org.apache.el;  type="internal", \
 org.apache.el.lang; type="internal", \
 org.apache.el.util; type="internal", \
 org.apache.el.stream; type="internal"
IBM-ShortName: el-3.0
Subsystem-Version: 3.0.0
Subsystem-Name: Expression Language 3.0
-features=com.ibm.websphere.appserver.javax.el-3.0
-bundles=com.ibm.ws.org.apache.jasper.el.3.0
kind=ga
edition=core
WLP-Activation-Type: parallel
