-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jmsMdb-3.2
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: jmsMdb-3.2
Subsystem-Name: JMS Message-Driven Beans 3.2
-features=com.ibm.websphere.appserver.transaction-1.2, \
  com.ibm.websphere.appserver.mdb-3.2
-bundles=com.ibm.ws.ejbcontainer.mdb
kind=ga
edition=base
superseded-by=mdb-3.2
