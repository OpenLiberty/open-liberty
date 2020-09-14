-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.certificateCreator-2.0
visibility=private
singleton=true
Subsystem-Version: 2.0.0
IBM-App-ForceRestart: install, \
 uninstall
-bundles=com.ibm.ws.crypto.certificate.creator.acme
kind=ga
edition=base
