-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.persistence-2.6
singleton=true
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.javax.persistence.base-2.1
-bundles=com.ibm.websphere.appserver.thirdparty.eclipselink; apiJar=false; location:=dev/api/third-party/
kind=ga
edition=core

