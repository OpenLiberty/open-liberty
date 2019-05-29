The file:

publish/features/featureset_anno.blst

Must have the format:

com.ibm.ws.anno_.*

And not:

com.ibm.ws.anno_*

Note the missing "." in the text that fails.

When the "." is missing, bundle resolution fails with the message:
 
[ERROR   ] CWWKF0002E: A bundle could not be found for 'com.ibm.ws.anno_*'.

Open design issue
