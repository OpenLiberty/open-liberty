-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.el-2.2
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-6.0; ibm.tolerates:="7.0, 8.0"
-bundles=com.ibm.websphere.javaee.el.2.2; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.el:el-api:2.2"
kind=ga
edition=core
