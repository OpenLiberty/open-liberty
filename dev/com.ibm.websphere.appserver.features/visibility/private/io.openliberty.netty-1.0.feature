-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.netty-1.0
Subsystem-Name: Netty internal implementation 1.0
singleton=true
-features=\
  io.openliberty.io.netty, \
  io.openliberty.io.netty.ssl
-bundles=\
  com.ibm.ws.wsbytebuffer, \
  io.openliberty.endpoint, \
  io.openliberty.netty.internal, \
  io.openliberty.netty.internal.impl
kind=noship
edition=core
WLP-Activation-Type: parallel
