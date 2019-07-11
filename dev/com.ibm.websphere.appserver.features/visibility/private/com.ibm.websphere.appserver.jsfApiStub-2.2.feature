-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsfApiStub-2.2
visibility=private
IBM-ShortName: jsfApiStub-2.2
IBM-App-ForceRestart: install, uninstall
Subsystem-Content: com.ibm.ws.jsf.2.2; type="jar"; location:="dev/api/third-party/"
Subsystem-Type: osgi.subsystem.feature
kind=ga
edition=core