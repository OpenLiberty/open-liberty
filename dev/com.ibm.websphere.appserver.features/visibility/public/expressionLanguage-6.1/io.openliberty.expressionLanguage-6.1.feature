-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.expressionLanguage-6.1
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: jakarta.el; type="spec", \
 org.apache.el;  type="internal", \
 org.apache.el.lang; type="internal", \
 org.apache.el.util; type="internal", \
 org.apache.el.stream; type="internal"
IBM-ShortName: expressionLanguage-6.1
WLP-AlsoKnownAs: el-6.1
Subsystem-Version: 6.1.0
Subsystem-Name: Jakarta Expression Language 6.1
-features=io.openliberty.jakarta.expressionLanguage-6.1, \
  com.ibm.websphere.appserver.eeCompatible-12.0
-bundles=io.openliberty.org.apache.jasper.expressionLanguage.6.0
kind=noship
edition=full
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: jakartaee-12.0
