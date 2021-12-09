-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.ejbHome-3.2
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: ejbHome-3.2
Subsystem-Name: Enterprise JavaBeans Home Interfaces 3.2
-features=com.ibm.websphere.appserver.transaction-1.2, \
  com.ibm.websphere.appserver.ejbLite-3.2
-bundles=com.ibm.ws.ejbcontainer.ejb2x
kind=ga
edition=base
