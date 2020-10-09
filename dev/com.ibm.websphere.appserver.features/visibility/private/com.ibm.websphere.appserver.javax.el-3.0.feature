-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.el-3.0
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-bundles=com.ibm.websphere.javaee.el.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.el:javax.el-api:3.0.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
