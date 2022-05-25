-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.expressionLanguage-5.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: jakarta.el; type="spec", \
 org.apache.el;  type="internal", \
 org.apache.el.lang; type="internal", \
 org.apache.el.util; type="internal", \
 org.apache.el.stream; type="internal"
IBM-ShortName: expressionLanguage-5.0
WLP-AlsoKnownAs: el-5.0
Subsystem-Version: 5.0.0
Subsystem-Name: Jakarta Expression Language 5.0
-features=io.openliberty.jakarta.expressionLanguage-5.0, \
  com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=io.openliberty.org.apache.jasper.expressionLanguage.5.0
kind=noship
edition=full
WLP-Activation-Type: parallel
