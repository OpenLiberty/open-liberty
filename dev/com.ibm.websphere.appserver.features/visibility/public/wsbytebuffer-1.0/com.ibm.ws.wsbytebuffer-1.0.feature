-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.ws.wsbytebuffer-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
  uninstall
IBM-ShortName: wsbytebuffer-1.0
Subsystem-Name: Websphere WsByteBuffer SPI 1.0
-bundles=\
  com.ibm.ws.wsbytebuffer, \
  com.ibm.ws.timer
kind=noship
edition=full
WLP-Activation-Type: parallel