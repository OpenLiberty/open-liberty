-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.springBootHandler-4.0
visibility=private
-features=io.openliberty.jakartaeePlatform-11.0,\
	com.ibm.websphere.appserver.eeCompatible-11.0
-bundles=\
 com.ibm.ws.app.manager.springboot.jakarta, \
 io.openliberty.springboot.support.shutdown.version40, \
 com.ibm.ws.springboot.utility
-files=\
 bin/tools/ws-springbootutil.jar, \
 bin/springBootUtility; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 bin/springBootUtility.bat
kind=beta
edition=core
singleton=true
IBM-API-Package: \
 com.ibm.ws.app.manager.springboot.container.config; type="internal", \
 com.ibm.ws.app.manager.springboot.container; type="internal"
