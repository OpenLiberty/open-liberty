-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.appClientSupport-2.0
visibility=public
IBM-ShortName: appClientSupport-2.0
Subsystem-Name: Jakarta Application Client Support for Server 2.0
-features=io.openliberty.appclient.appClient-2.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  com.ibm.websphere.appserver.clientContainerRemoteSupport-1.0, \
  com.ibm.websphere.appserver.injection-2.0
kind=beta
edition=base
WLP-Activation-Type: parallel
