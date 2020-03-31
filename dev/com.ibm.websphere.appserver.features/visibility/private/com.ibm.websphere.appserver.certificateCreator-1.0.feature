-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.certificateCreator-1.0
visibility=private
singleton=true
Subsystem-Version: 1.0.0
IBM-App-ForceRestart: install, \
 uninstall
-bundles=com.ibm.ws.crypto.certificate.creator.selfsigned
kind=ga
edition=core
