-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.globalhandler-1.0
visibility=protected
-features=\
  io.openliberty.globalhandler1.0.internal.ee-10.0, \
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0,5.0,6.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
