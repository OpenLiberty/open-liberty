
# count init
 - reader open
   - use checkpoint for reader positioning
# count update
   - reader checkpoint
   - in writelistener
      - update persistent with new aggregate count
      - update in exit status too 
# count aggregate
   - in analyzer, aggregate exit status into big exit status
# count validate
 - afterStep:  look at exit status for aggregate

# count is in persistent userdata... updated by reader exitstatus in partition threads as well to take advantage of analyzer

# STEP level has overall count in persistent userdata

# partition analyzer has convenient exitStatus
# no end of partition, so keep update in exitStatus as well

