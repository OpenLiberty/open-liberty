-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.globalhandler1.0.internal.ee-12.0
singleton=true
visibility = private
-features=\
  com.ibm.websphere.appserver.servlet-6.2
-bundles=\
  io.openliberty.webservices.handler
kind=noship
edition=full
WLP-Activation-Type: parallel