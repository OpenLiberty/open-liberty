-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.activation.internal-2.1
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client, \
 server
IBM-API-Package: jakarta.activation; type="spec"
-features=io.openliberty.activation.impl-2.1
kind=ga
edition=core
WLP-Activation-Type: parallel
