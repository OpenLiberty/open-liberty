-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.globalhandler1.0.internal.ee-10.0
singleton=true
visibility = private
-features=\
  com.ibm.websphere.appserver.servlet-6.0
-bundles=\
  io.openliberty.webservices.handler
kind=ga
edition=core
WLP-Activation-Type: parallel