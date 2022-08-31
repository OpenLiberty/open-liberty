-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.telemetry-1.0
visibility=private
singleton=true
-features=io.openliberty.mpCompatible-6.0; ibm.tolerates:="5.0"
-bundles=io.openliberty.io.opentelemetry; location:="dev/api/stable/,lib/"
kind=beta
edition=core
WLP-Activation-Type: parallel
