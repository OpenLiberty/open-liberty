-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.adminCenter-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-ShortName: adminCenter-1.0
Subsystem-Name: Admin Center
Subsystem-Icon: OSGI-INF/admincenter_200x200.png,OSGI-INF/admincenter_200x200.png;size=200
-features=com.ibm.websphere.appserver.restHandler-1.0, \
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0,5.0,6.0,6.1", \
  com.ibm.websphere.appserver.adminCenter.tool.explore-1.0, \
  com.ibm.websphere.appserver.adminCenter.tool.serverConfig-1.0, \
  io.openliberty.adminCenter1.0.internal.ee-6.0; ibm.tolerates:="9.0,10.0,11.0"
-bundles=\
    com.ibm.ws.ui,\
    io.openliberty.jsonsupport.internal,\
    com.ibm.ws.org.joda.time.1.6.2, \
    io.openliberty.com.google.gson
kind=ga
edition=core
