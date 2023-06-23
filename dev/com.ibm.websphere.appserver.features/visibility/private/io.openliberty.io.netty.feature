-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.io.netty
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-bundles=io.openliberty.io.netty; location:="lib/";
kind=noship
edition=full
WLP-Activation-Type: parallel
