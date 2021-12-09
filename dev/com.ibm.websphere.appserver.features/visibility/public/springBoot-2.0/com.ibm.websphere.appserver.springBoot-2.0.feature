-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.springBoot-2.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-ShortName: springBoot-2.0
IBM-Process-Types: server
Subsystem-Name: Spring Boot Support 2.0
-features=com.ibm.websphere.appserver.springBootHandler-1.0, \
  com.ibm.websphere.appserver.eeCompatible-7.0; ibm.tolerates:="8.0, 6.0"
kind=ga
edition=core
