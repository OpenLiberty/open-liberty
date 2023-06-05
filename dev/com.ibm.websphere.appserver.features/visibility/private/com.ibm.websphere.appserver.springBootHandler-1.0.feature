-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.springBootHandler-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=private
-features=com.ibm.websphere.appserver.javaeePlatform-7.0
-bundles=\
 com.ibm.ws.app.manager.springboot, \
 com.ibm.ws.springboot.support.shutdown, \
 com.ibm.ws.springboot.utility
-files=\
 bin/tools/ws-springbootutil.jar, \
 bin/springBootUtility; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 bin/springBootUtility.bat
kind=ga
edition=core
singleton=true
IBM-API-Package: \
 com.ibm.ws.app.manager.springboot.container.config; type="internal", \
 com.ibm.ws.app.manager.springboot.container; type="internal"
