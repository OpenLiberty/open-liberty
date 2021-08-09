package com.ibm.websphere.microprofile.faulttolerance_fat.validation;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Bulkhead;

@ApplicationScoped
public class BulkheadQueueNegative {

    @Bulkhead(waitingTaskQueue = -1)
    public void badMethod() {}

}
