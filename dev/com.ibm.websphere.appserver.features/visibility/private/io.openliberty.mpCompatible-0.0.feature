#mpCompatible-0.0 means "everything prior to MicroProfile 4.0"
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpCompatible-0.0
visibility=private
singleton=true
kind=ga
edition=core
WLP-DisableAllFeatures-OnConflict: false
WLP-Activation-Type: parallel
