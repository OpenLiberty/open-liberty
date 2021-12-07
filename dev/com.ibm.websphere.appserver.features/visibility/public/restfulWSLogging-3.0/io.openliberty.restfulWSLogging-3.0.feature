-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restfulWSLogging-3.0
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: restfulWSLogging-3.0
Subsystem-Name: Jakarta RESTful Web Services 3.0 with HTTP logging
-features=io.openliberty.restfulWS-3.0; ibm.tolerates:="3.1"
-bundles=\
 io.openliberty.restfulWS.internal.logging.filter.jakarta
kind=beta
edition=core
WLP-Activation-Type: parallel
