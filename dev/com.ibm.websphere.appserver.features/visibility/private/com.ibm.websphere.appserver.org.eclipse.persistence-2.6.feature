-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.persistence-2.6
WLP-DisableAllFeatures-OnConflict: false
singleton=true
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.javax.persistence.base-2.1
-bundles=com.ibm.websphere.appserver.thirdparty.eclipselink; apiJar=false; location:=dev/api/third-party/; mavenCoordinates="org.eclipse.persistence:eclipselink:2.6.0"
kind=ga
edition=core

WLP-Activation-Type: parallel
