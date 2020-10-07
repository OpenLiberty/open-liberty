-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.ldapRegistry-3.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-ShortName: ldapRegistry-3.0
Subsystem-Name: LDAP User Registry 3.0
-features=\
  com.ibm.websphere.appserver.federatedRegistry-1.0
-bundles=com.ibm.ws.security.wim.adapter.ldap
kind=ga
edition=core
