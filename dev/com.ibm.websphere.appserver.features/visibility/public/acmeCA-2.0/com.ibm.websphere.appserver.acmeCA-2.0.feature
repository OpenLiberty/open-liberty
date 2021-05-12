-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.acmeCA-2.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-ShortName: acmeCA-2.0
Subsystem-Version: 2.0
Subsystem-Name: Automatic Certificate Management Environment (ACME) Support 2.0
-features=\
  com.ibm.websphere.appserver.servlet-4.0; ibm.tolerates:="3.1,5.0", \
  com.ibm.websphere.appserver.internal.slf4j-1.7.7, \
  com.ibm.websphere.appserver.transportSecurity-1.0, \
  com.ibm.wsspi.appserver.webBundle-1.0, \
  com.ibm.websphere.appserver.certificateCreator-2.0, \
  com.ibm.websphere.appserver.restHandler-1.0, \
  io.openliberty.acmeCA2.0.internal.ee-7.0; ibm.tolerates:=9.0
kind=ga
edition=base
