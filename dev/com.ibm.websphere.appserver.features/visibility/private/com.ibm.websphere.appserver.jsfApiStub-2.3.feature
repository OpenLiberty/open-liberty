-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsfApiStub-2.3
visibility=private
IBM-ShortName: jsfApiStub-2.3
IBM-App-ForceRestart: install, uninstall
Subsystem-Content: com.ibm.ws.org.apache.myfaces.2.3; type="jar"; location:="dev/api/third-party/"
Subsystem-Type: osgi.subsystem.feature
kind=ga
edition=core
WLP-Activation-Type: parallel
