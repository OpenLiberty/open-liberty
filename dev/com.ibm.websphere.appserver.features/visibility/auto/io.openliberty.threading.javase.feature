-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.threading.javase
visibility=private
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature))"
-bundles=\
  io.openliberty.threading.internal.java20orless; require-java:="[8,21)", \
  io.openliberty.threading.internal.java21; require-java:=21
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
