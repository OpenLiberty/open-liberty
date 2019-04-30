-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.jaxws-2.3
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
Subsystem-Name: Java Web Services API 2.3
-features=\
 com.ibm.websphere.appserver.jaxb-2.3
kind=noship
edition=full
