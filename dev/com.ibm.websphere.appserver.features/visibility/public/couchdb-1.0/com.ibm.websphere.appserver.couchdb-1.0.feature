-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.couchdb-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-ShortName: couchdb-1.0
Subsystem-Name: CouchDB Integration 1.0
-features=com.ibm.websphere.appserver.appLifecycle-1.0, \
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.eeCompatible-6.0; ibm.tolerates:="7.0, 8.0, 9.0, 10.0"
-bundles=com.ibm.ws.couchdb
kind=ga
edition=base
