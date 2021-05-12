-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.appSecurity-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: appSecurity-1.0
Subsystem-Version: 1.1.0
Subsystem-Name: Application Security 1.0
-features=com.ibm.websphere.appserver.ldapRegistry-3.0, \
 com.ibm.websphere.appserver.appSecurity-2.0, \
 com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1, 4.0", \
 com.ibm.websphere.appserver.eeCompatible-6.0; ibm.tolerates:="7.0, 8.0"
kind=ga
edition=core
superseded-by=appSecurity-2.0,[servlet-3.0],[ldapRegistry-3.0]