-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.activation.internal-2.2
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client, \
 server
IBM-API-Package: jakarta.activation; type="spec"
-features=io.openliberty.activation.impl-2.2
kind=noship
edition=full
WLP-Activation-Type: parallel
