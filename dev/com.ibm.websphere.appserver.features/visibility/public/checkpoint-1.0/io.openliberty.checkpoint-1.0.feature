-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.checkpoint-1.0
visibility=public
singleton=true
IBM-ShortName: checkpoint-1.0
IBM-Process-Types: server
Subsystem-Name: Checkpoint and Restore Support 1.0
-bundles=io.openliberty.checkpoint,\
 io.openliberty.jigawatts,\
 io.openliberty.checkpoint.openj9
kind=noship
edition=full
WLP-Activation-Type: parallel
