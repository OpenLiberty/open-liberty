-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxws-2.3
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: jaxws-2.3
Subsystem-Name: Java Web Services 2.3
-features=\
 com.ibm.websphere.appserver.jaxb-2.3, \
 com.ibm.websphere.appserver.internal.jaxws-2.3
kind=noship
edition=full
