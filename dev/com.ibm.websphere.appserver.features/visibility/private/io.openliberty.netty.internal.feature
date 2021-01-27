-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.netty.internal
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-bundles=io.openliberty.netty.internal; location:="lib/";
kind=noship
edition=core
WLP-Activation-Type: parallel
