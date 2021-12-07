-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.messaging-3.0.internal
singleton=true
IBM-App-ForceRestart: uninstall
-features=io.openliberty.messaging.internal-3.0, \
  com.ibm.websphere.appserver.transaction-2.0
kind=ga
edition=base
WLP-Activation-Type: parallel
