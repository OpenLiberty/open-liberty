Last update: 2014/06/10

This project (com.ibm.ws.jbatch.utility) implements the bin/batchManager command-line utility. 

Currently the exposed tasks are: ...

*NOTE* Only extend this functionality judiciously. To add new task see below.

-------------------------------------------------------------------------------

To add a new task to the script:

1. Create a class in: com.ibm.ws.jbatch.utility.tasks
2. Register it in the JBatchUtility main() method.
   Note the order in which it is registered will dictacte the order it is
   listed in help.
