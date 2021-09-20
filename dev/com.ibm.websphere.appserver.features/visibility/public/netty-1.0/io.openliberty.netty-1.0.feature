-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.netty-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
  uninstall
IBM-ShortName: netty-1.0
Subsystem-Name: Netty Transport Framework 1.0
-features=com.ibm.websphere.appserver.channelfw-1.0, \
  io.openliberty.io.netty.ssl, \
  io.openliberty.io.netty
-bundles=\
  com.ibm.ws.wsbytebuffer, \
  io.openliberty.netty.internal, \
  io.openliberty.netty.internal.impl
kind=noship
edition=full
WLP-Activation-Type: parallel
