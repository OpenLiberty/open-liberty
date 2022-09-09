-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.nosqlUnused-1.0
visibility=public
singleton=true
IBM-ShortName: nosqlUnused-1.0
Subsystem-Name: Unused feature to satisfy FeatureListValidator 1.0
# TODO remove this unused feature once we no longer need to transform the Jakarta NoSQL spec from javax packages that the FeatureListValidator expects to see used by a feature
-bundles=io.openliberty.javax.nosql-1.0
kind=noship
edition=full
WLP-Activation-Type: parallel
