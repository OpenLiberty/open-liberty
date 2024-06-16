-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.eeCompatible
visibility=private
singleton=true
-features= \
    com.ibm.websphere.appserver.eeCompatible-0.0; ibm.tolerates:="6.0,7.0,8.0,9.0,10.0,11.0"
 kind=ga
edition=core
