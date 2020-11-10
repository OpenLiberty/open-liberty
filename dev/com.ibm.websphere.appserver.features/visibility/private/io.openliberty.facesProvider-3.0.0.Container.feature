# This private feature corresponds to using a JSF-providing feature
# with just a container and no implementation (e.g. facesContainer-3.0)
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.facesProvider-3.0.0.Container
singleton=true
visibility=private
kind=beta
edition=core
WLP-Activation-Type: parallel
