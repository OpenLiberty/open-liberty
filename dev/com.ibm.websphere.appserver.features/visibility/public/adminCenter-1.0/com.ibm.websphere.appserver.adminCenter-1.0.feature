-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.adminCenter-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-ShortName: adminCenter-1.0
Subsystem-Name: Admin Center
Subsystem-Icon: OSGI-INF/admincenter_200x200.png,OSGI-INF/admincenter_200x200.png;size=200
-features=com.ibm.websphere.appserver.restHandler-1.0, \
  com.ibm.wsspi.appserver.webBundle-1.0, \
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0,5.0,6.0", \
  com.ibm.websphere.appserver.adminCenter.tool.explore-1.0, \
  com.ibm.websphere.appserver.adminCenter.tool.serverConfig-1.0, \
  io.openliberty.adminCenter1.0.internal.ee-6.0; ibm.tolerates:="9.0,10.0"
-bundles=\
    com.ibm.websphere.jsonsupport,\
    com.ibm.ws.org.joda.time.1.6.2
kind=ga
edition=core
