#mpCompatible-0.0 means "everything prior to MicroProfile 4.0"
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpCompatible-0.0
visibility=private
singleton=true
-features=io.openliberty.internal.mpVersion-1.0; ibm.tolerates:="1.2,1.3,1.4,2.0,2.1,2.2,3.0,3.2,3.3"
kind=ga
edition=core
WLP-DisableAllFeatures-OnConflict: false
WLP-Activation-Type: parallel
