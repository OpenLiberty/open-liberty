# This private feature corresponds to using a JSF-providing feature
# with the Apache MyFaces implementation
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsfProvider-2.2.0.MyFaces
WLP-DisableAllFeatures-OnConflict: false
singleton=true
visibility=private
kind=ga
edition=core
