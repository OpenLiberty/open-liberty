-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.httpcommons-1.0
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
-bundles=com.ibm.ws.org.apache.httpcomponents, \
 com.ibm.ws.org.apache.httpcomponents
kind=ga
edition=core
