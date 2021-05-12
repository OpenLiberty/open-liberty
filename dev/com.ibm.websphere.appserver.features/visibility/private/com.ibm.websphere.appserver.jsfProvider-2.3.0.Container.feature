# This private feature corresponds to using a JSF-providing feature
# with just a container and no implementation (e.g. jsfContainer-2.3)
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsfProvider-2.3.0.Container
WLP-DisableAllFeatures-OnConflict: false
singleton=true
visibility=private
kind=ga
edition=core
