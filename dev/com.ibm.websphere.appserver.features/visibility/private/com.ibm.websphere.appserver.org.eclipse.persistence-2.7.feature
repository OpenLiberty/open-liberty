-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.persistence-2.7
singleton=true
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.javax.persistence.base-2.2
-bundles=com.ibm.websphere.appserver.thirdparty.eclipselink.2.7; apiJar=false; location:=dev/api/third-party/; mavenCoordinates="org.eclipse.persistence:eclipselink:2.7.1"
kind=ga
edition=core
WLP-Activation-Type: parallel
